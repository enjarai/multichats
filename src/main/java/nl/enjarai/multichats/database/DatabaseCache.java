package nl.enjarai.multichats.database;

import nl.enjarai.multichats.types.Group;

import java.util.*;

public class DatabaseCache {

    private final long timeToLive;
    private final HashMap<UUID, CacheObject> cacheMap = new HashMap<>();
    private final ArrayList<Group> deleteGroups = new ArrayList<>();

    protected class CacheObject {

        public long lastAccessed = System.currentTimeMillis();
        public Group primaryGroup;
        public List<Group> groups;

        protected CacheObject(Group primaryGroup, List<Group> groups) {
            this.primaryGroup = primaryGroup;
            this.groups = groups;
        }
    }

    public DatabaseCache(long timeToLive, final long timerInterval) {
        this.timeToLive = timeToLive * 1000;

        if (timeToLive > 0 && timerInterval > 0) {

            Thread t = new Thread(() -> {
                while (true) {
                    try {

                        Thread.sleep(timerInterval * 1000);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                    cleanup();
                }
            });

            t.setDaemon(true);
            t.start();
        }
    }

    public void put(UUID key, Group primary, List<Group> groups) {
        synchronized (cacheMap) {
            cacheMap.put(key, new CacheObject(primary, groups));
        }
    }

    private CacheObject get(UUID key) {
        synchronized (cacheMap) {
            CacheObject c = cacheMap.get(key);

            if (c == null) {
                return null;
            } else {
                c.lastAccessed = System.currentTimeMillis();
                return c;
            }
        }
    }

    public Group getPrimary(UUID key) {
        CacheObject c = get(key);

        if (c == null) {
            return null;
        } else {
            return c.primaryGroup;
        }

    }

    public List<Group> getGroups(UUID key) {
        CacheObject c = get(key);

        if (c == null) {
            return null;
        } else {
            return c.groups;
        }

    }

    public void removeByKey(UUID key) {
        synchronized (cacheMap) {
            cacheMap.remove(key);
        }
    }

    public void removeByGroup(Group group) {
        synchronized (deleteGroups) {
            deleteGroups.add(group);
        }
    }

    public int size() {
        synchronized (cacheMap) {
            return cacheMap.size();
        }
    }

    public void cleanup() {

        long now = System.currentTimeMillis();
        ArrayList<UUID> deleteKey;

        synchronized (cacheMap) {

            deleteKey = new ArrayList<>(cacheMap.size() + 1);
            UUID key;
            CacheObject c;

            for (Map.Entry<UUID, CacheObject> set : cacheMap.entrySet()) {
                key = set.getKey();
                c = set.getValue();

                if (c != null) {
                    if (now > (timeToLive + c.lastAccessed)) {

                        deleteKey.add(key);
                        break;
                    }

                    deleteGroupsLoop:
                    for (Group group : deleteGroups) {
                        if (Objects.equals(c.primaryGroup.name, group.name)) {
                            deleteKey.add(key);
                            break;
                        } else {
                            for (Group cGroup : c.groups) {
                                if (Objects.equals(cGroup.name, group.name)) {
                                    deleteKey.add(key);
                                    break deleteGroupsLoop;
                                }
                            }
                        }
                    }
                }
            }
        }

        for (UUID key : deleteKey) {
            synchronized (cacheMap) {

                cacheMap.remove(key);
            }

            Thread.yield();
        }
    }
}
