package org.geoserver.platform.resource;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Notification of resource changes.
 * <p>
 * Notification is provided as a delta modified resources:
 * <ul>
 * <li>Listeners on a single resource will be notified on resource change to that resource.
 * 
 * A listener to path="user_projections/epsg.properties" receive notification on change to the <b>epsg.properties</b> file. This notification will
 * consist of of delta=<code>user_projections/epsg.properties</code></li>
 * <li>Listeners on a directory will be notified on any resource change in the directory. The delta will include any modified directories.
 * 
 * A listener on path="style" is notified on change to <b>style/pophatch.sld</b> and <b>style/icons/city.png</b>. The change to these two files is
 * represented with delta consisting of delta=<code>style,style/icons,style/icons/city.png,style/pophatch.sld</code></li>
 * </ul>
 * <li>Removed resources may be represented in notification, but will have reverted to {@link Resource.Type#UNDEFINED} since the content is no longer
 * present.</li> </ul>
 * 
 * @author Jody Garnett (Boundless)
 */
public class ResourceNotification {
    private ResourceStore store;

    private List<String> delta;

    /**
     * Notification of a change to a single resource.
     * 
     * @param store
     * @param path
     */
    ResourceNotification(ResourceStore store, String path) {
        this.store = store;
        this.delta = Collections.singletonList(path);
    }
    public ResourceNotification( ResourceStore store, File baseDirectory, List<File> changedFiles ){
        this.store = store;
        this.delta = new ArrayList<String>(changedFiles.size());
        for( File changed : changedFiles ){
            String path = Paths.convert( baseDirectory, changed );
            delta.add(path);
        }
    }   

    /**
     * Notification changes to directory contents.
     * 
     * @param store
     * @param delta
     */
    ResourceNotification(ResourceStore store, List<String> delta) {
        this.store = store;
        List<String> sorted = new ArrayList<String>(delta);
        Collections.sort(sorted);
        this.delta = Collections.unmodifiableList(sorted);
    }

    /**
     * Paths of changed resources.
     * <p>
     * This list of changed resources is sorted and includes any relevant directories.
     * 
     * @return paths of changed resources
     */
    public List<String> delta() {
        return delta; // unmodifiable
    }

    /**
     * The first changed resource from {@link #delta()}.
     * 
     * @return first changed resource from {@link #delta()}
     */
    Resource resource() {
        String path = delta.get(0);
        return store.get(path);
    }

}