/* Copyright (c) 2014 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.util.logging.Logging;

/**
 * Utility class for File handling code. For additional utilities see IOUtils.
 * <p>
 * This utility class focuses on making file management tasks easier for
 * ResourceStore implementors.
 * 
 * @since 2.5
 */
public final class Files {
    
    /**
     * Quick Resource adaptor suitable for a single file.
     */
    private static final class ResourceAdaptor implements Resource {
        private final File file;

        private ResourceAdaptor(File file) {
            this.file = file;
        }

        @Override
        public String path() {
            return Paths.convert(file.getPath()); 
        }

        @Override
        public String name() {
            return file.getName();
        }

        @Override
        public Lock lock() {
            return new Lock() {
                public void release() {
                }
            };
        }

        @Override
        public InputStream in() {
            try {
                return new FileInputStream(file);
            } catch (FileNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public OutputStream out() {
            try {
                return new FileOutputStream(file);
            } catch (FileNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public File file() {
            return file;
        }

        @Override
        public File dir() {
            throw new IllegalStateException("Resource adaptor cannot be used to create directory");
        }

        @Override
        public long lastmodified() {
            return file.lastModified();
        }

        @Override
        public Resource parent() {
            throw new IllegalStateException("Resource adaptor dos not support parent()");
        }

        @Override
        public Resource get(String resourcePath) {
            throw new IllegalStateException();
        }

        @Override
        public List<Resource> list() {
            return Collections.emptyList();
        }

        @Override
        public Type getType() {
            return Type.RESOURCE;
        }

        @Override
        public String toString() {
            return "ResourceAdaptor("+file+")";
        }
    }

    private static final Logger LOGGER = Logging.getLogger(Files.class);
   
    private Files() {
        // utility class do not subclass
    }
    
    /**
     * Adapter allowing a File reference to be quickly used a Resource.
     * 
     * This is used as a placeholder when updating code to use resource, while still maintaining deprecated File methods: 
     * <pre></code>
     * //deprecated
     * public FileWatcher( File file ){
     *    this.resource = Files.asResource( file );
     * }
     * //deprecated
     * public FileWatcher( Resource resource ){
     *    this.resource = resource;    
     * }
     * </code></pre>
     * Note this only an adapter for single files (not directories).
     * 
     * @param file File (not a directory) to adapt as a Resource
     * @return resource adaptor for provided file
     */
    public static Resource asResource(final File file ){
        if( file == null || !file.exists() ){
            throw new IllegalArgumentException("File required");
        }
        if( file.isDirectory() ){
            throw new IllegalArgumentException("File required (not a directory)");
        }
        return new ResourceAdaptor(file);
    }
    /**
     * Safe buffered output stream to temp file, output stream close used to renmae file into place.
     * 
     * @param file
     * @return buffered output stream to temporary file (output stream close used to rename file into place)
     * @throws FileNotFoundException
     */
    public static OutputStream out(final File file) throws FileNotFoundException {
        // first save to a temp file
        final File temp = new File(file.getParentFile(), file.getName() + ".tmp");

        if (temp.exists()) {
            temp.delete();
        }
        return new OutputStream() {
            FileOutputStream delegate = new FileOutputStream(temp);

            @Override
            public void close() throws IOException {
                delegate.close();
                // no errors, overwrite the original file
                Files.move(temp, file);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                delegate.write(b, off, len);
            }

            @Override
            public void flush() throws IOException {
                delegate.flush();
            }

            @Override
            public void write(byte[] b) throws IOException {
                delegate.write(b);
            }

            @Override
            public void write(int b) throws IOException {
                delegate.write(b);
            }
        };
    }

    /**
     * Moves (or renames) a file.
     *  
     * @param source The file to rename.
     * @param dest The file to rename to. 
     * @return <code>true</code> if source file moved to dest
     */
    public static boolean move( File source, File dest ) throws IOException {
        if( source == null || !source.exists()){
            throw new NullPointerException("File source required");
        }
        if( dest == null ){
            throw new NullPointerException("File dest required");
        }
        // same path? Do nothing
        if (source.getCanonicalPath().equalsIgnoreCase(dest.getCanonicalPath())){
            return true;
        }

        // windows needs special treatment, we cannot rename onto an existing file
        boolean win = System.getProperty("os.name").startsWith("Windows");
        if ( win && dest.exists() ) {
            // windows does not do atomic renames, and can not rename a file if the dest file
            // exists
            if (!dest.delete()) {
                throw new IOException("Failed to move " + source.getAbsolutePath() + " - unable to remove existing: " + dest.getCanonicalPath());
            }
        }
        // make sure the rename actually succeeds
        if(!source.renameTo(dest)) {
            throw new IOException("Failed to move " + source.getAbsolutePath() + " to " + dest.getAbsolutePath());
        }
        return true;
    }
    
    /**
     * Easy to use file delete (works for both files and directories).
     * 
     * Recursively deletes the contents of the specified directory, 
     * and finally wipes out the directory itself. For each
     * file that cannot be deleted a warning log will be issued.
     * 
     * @param file File to remove
     * @throws IOException
     * @returns true if any file present is removed
     */
    public static boolean delete(File file) {
        if( file == null || !file.exists() ){
            return true; // already done
        }
        if( file.isDirectory()){
            emptyDirectory(file);    
        }
        return file.delete();
    }

    /**
     * Recursively deletes the contents of the specified directory 
     * (but not the directory itself). For each
     * file that cannot be deleted a warning log will be issued.
     * 
     * @param dir
     * @throws IOException
     * @returns true if all the directory contents could be deleted, false otherwise
     */
    private static boolean emptyDirectory(File directory) {
        if (!directory.isDirectory()){
            throw new IllegalArgumentException(directory
                    + " does not appear to be a directory at all...");
        }

        boolean allClean = true;
        File[] files = directory.listFiles();

        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                allClean &= delete(files[i]);
            } else {
                if (!files[i].delete()) {
                    LOGGER.log(Level.WARNING, "Could not delete {0}", files[i].getAbsolutePath());
                    allClean = false;
                }
            }
        }
        
        return allClean;
    }
    
}