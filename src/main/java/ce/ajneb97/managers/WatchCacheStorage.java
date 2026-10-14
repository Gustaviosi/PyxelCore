package ce.ajneb97.managers;
import ce.ajneb97.ConditionalEvents;
import java.io.File;
import java.sql.*;
import java.util.*;
public class WatchCacheStorage {
    private static WatchCacheStorage INSTANCE;
    public static WatchCacheStorage get(ConditionalEvents p){
        if(INSTANCE == null){ INSTANCE = new WatchCacheStorage(p); INSTANCE.open(); }
        return INSTANCE;
    }
    private final ConditionalEvents plugin;
    private Connection conn;
    private WatchCacheStorage(ConditionalEvents plugin){ this.plugin = plugin; }
    private boolean open(){
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:" + new File(plugin.getDataFolder(), "watch_cache.db").getAbsolutePath());
            try (Statement st = conn.createStatement()){
                st.execute("CREATE TABLE IF NOT EXISTS watch_cache (uuid TEXT, event TEXT, value TEXT, PRIMARY KEY(uuid,event))");
            }
            return true;
        } catch(Exception ex){ plugin.getLogger().warning("[Watch] " + ex.getMessage()); return false; }
    }
    public Map<UUID,String> loadForEvent(String event){
        Map<UUID,String> m = new HashMap<>();
        if(conn == null) return m;
        try (PreparedStatement ps = conn.prepareStatement("SELECT uuid,value FROM watch_cache WHERE event=?")){
            ps.setString(1,event);
            try (ResultSet rs = ps.executeQuery()){
                while(rs.next()){ try { m.put(UUID.fromString(rs.getString(1)), rs.getString(2)); } catch(Exception ignored){} }
            }
        } catch(Exception ignored){}
        return m;
    }
    public void put(UUID uuid, String event, String value){
        if(conn == null) return;
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO watch_cache(uuid,event,value) VALUES(?,?,?) ON CONFLICT(uuid,event) DO UPDATE SET value=excluded.value")){
            ps.setString(1,uuid.toString()); ps.setString(2,event); ps.setString(3,value); ps.executeUpdate();
        } catch(Exception ignored){}
    }
}