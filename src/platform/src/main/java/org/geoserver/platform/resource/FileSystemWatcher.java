package org.geoserver.platform.resource;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Active object (using a ScheduledExecutorService) used to watch file system for changes.
 * <p>
 * This implementation currently polls the file system and should be updated with Java 7 WatchService when available.
 * 
 * @author Jody Garnett (Boundless)
 */
public class FileSystemWatcher {
    /**
     * Record of a ResourceListener that wishes to be notified
     * of changes to a path.
     */
    private class Watch implements Comparable<Watch>{
        final String path;
        final ResourceListener listener;
        final File file;
        
        long checked = 0;    // time watch was last checked
        File[] contents; // directory contents at last check
        
        public Watch(String path, ResourceListener listener){
            this.path = path;
            this.listener = listener;
            this.file = Paths.toFile(store.baseDirectory, path );            
            checked = file.exists() ? System.currentTimeMillis() : 0;
            if( file.isDirectory() ){
                contents = file.listFiles();
            }
        }
        
        public ResourceListener getListener() {
            return listener;
        }
        public String getPath() {
            return path;
        }
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((listener == null) ? 0 : listener.hashCode());
            result = prime * result + ((path == null) ? 0 : path.hashCode());
            return result;
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Watch other = (Watch) obj;
            if (listener == null) {
                if (other.listener != null)
                    return false;
            } else if (!listener.equals(other.listener))
                return false;
            if (path == null) {
                if (other.path != null)
                    return false;
            } else if (!path.equals(other.path))
                return false;
            return true;
        }
        @Override
        public String toString() {
            return "Watch [path=" + path + ", listener=" + listener + "]";
        }
        @Override
        public int compareTo(Watch other) {
            return path.compareTo(other.path);
        }
        public List<File> changed(long now) {
            if( !file.exists() ){
                if( checked != 0 ){
                    checked = 0; // file has been deleted!
                    return Collections.singletonList(file);                    
                }
                else {
                    return Collections.emptyList();
                }
            }
            
            long mark = checked;
            checked = now;
            
            
            if( file.isFile() ){
                if( file.lastModified() > mark ){
                    return Collections.singletonList(file);
                }
            }
            if( file.isDirectory() ){
                File[] checkedFiles = this.contents;
                this.contents = file.listFiles();
                
                List<File> delta = new ArrayList<File>(this.contents.length);
                delta.addAll( Arrays.asList(checkedFiles));
                delta.removeAll( Arrays.asList(this.contents) );
                
                // check directory!
                if( file.lastModified() > mark ){
                    return Collections.singletonList(file);
                }
                
                // check contents
                for( File check : this.contents ){
                    if( check.lastModified() > mark ){
                        delta.add(file);                        
                    }
                }                
                return delta;
            }
            return Collections.emptyList();
        }        
    }
    private ScheduledExecutorService pool;
    private FileSystemResourceStore store;
    
    protected long lastmodified;
    
    CopyOnWriteArrayList<Watch> listeners = new CopyOnWriteArrayList<Watch>();

    private Runnable sync = new Runnable() {
        @Override
        public void run() {
            long now = System.currentTimeMillis();
            for (Watch watch : listeners) {
                List<File> changedFiles = watch.changed(now);
                if (!changedFiles.isEmpty()) {
                    ResourceNotification notify = new ResourceNotification(store,
                            store.baseDirectory, changedFiles);
                    try {
                        watch.listener.changed(notify);
                    } catch (Throwable t) {
                    }
                }
            }
        }
    };
    
    private ScheduledFuture<?> monitor;
    private TimeUnit unit = TimeUnit.SECONDS;
    private long delay = 30;
    
    FileSystemWatcher( FileSystemResourceStore store ){
        this.store = store;
        this.pool  = Executors.newSingleThreadScheduledExecutor();
    }
    public synchronized void addListener( String path, ResourceListener listener ){
        Watch watch = new Watch( path, listener);        
        if( !listeners.contains(watch) ){
            listeners.add(watch); // already listening!
            if( !listeners.isEmpty() ){
                monitor = pool.scheduleWithFixedDelay(sync, delay, delay, unit);
            }
        }
    }
    public synchronized void removeListener( String path, ResourceListener listener ){
        Watch watch = new Watch( path, listener);        
        boolean removed = listeners.remove(watch);
        if( removed && listeners.isEmpty() ){
            if( monitor != null ){
                monitor.cancel(false); // stop watching nobody is looking
            }
        }
    }
    
    /**
     * Package visibility to allow test cases to set a shorter delay for testing.
     * @param delay
     * @param unit
     */
    void schedule( long delay, TimeUnit unit ){
        this.delay = delay;
        this.unit = unit;
        if( monitor != null ){
            monitor.cancel(false);
            monitor = pool.schedule(sync,  delay,  unit );
        }
    }
}

