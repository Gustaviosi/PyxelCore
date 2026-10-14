package ce.ajneb97.variables;
import ce.ajneb97.ConditionalEvents;
import java.io.File;
import java.sql.*;
import java.util.*;
public class SQLiteVariablesStorage {
    private final ConditionalEvents plugin;
    private Connection conn;
    public SQLiteVariablesStorage(ConditionalEvents plugin){ this.plugin = plugin; }
    public boolean open(){
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:" + new File(plugin.getDataFolder(), "variables.db").getAbsolutePath());
            try (Statement st = conn.createStatement()){
                st.execute("CREATE TABLE IF NOT EXISTS globals (name TEXT PRIMARY KEY, value TEXT)");
                st.execute("CREATE TABLE IF NOT EXISTS players (uuid TEXT, name TEXT, value TEXT, PRIMARY KEY(uuid,name))");
            }
            return true;
        } catch(Exception ex){ plugin.getLogger().warning("[Vars/SQLite] " + ex.getMessage()); return false; }
    }
    public void close(){ try { if(conn != null) conn.close(); } catch(Exception ignored){} }
    public Map<String,String> loadGlobals(){
        Map<String,String> m = new HashMap<>();
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT name,value FROM globals")){
            while(rs.next()) m.put(rs.getString(1), rs.getString(2));
        } catch(Exception ignored){}
        return m;
    }
    public Map<UUID,Map<String,String>> loadPlayers(){
        Map<UUID,Map<String,String>> m = new HashMap<>();
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT uuid,name,value FROM players")){
            while(rs.next()){
                UUID u; try { u = UUID.fromString(rs.getString(1)); } catch(Exception e){ continue; }
                m.computeIfAbsent(u, k -> new HashMap<>()).put(rs.getString(2), rs.getString(3));
            }
        } catch(Exception ignored){}
        return m;
    }
    public void putGlobal(String name, String value){
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO globals(name,value) VALUES(?,?) ON CONFLICT(name) DO UPDATE SET value=excluded.value")){
            ps.setString(1,name); ps.setString(2,value); ps.executeUpdate();
        } catch(Exception ignored){}
    }
    public void putPlayer(UUID uuid, String name, String value){
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO players(uuid,name,value) VALUES(?,?,?) ON CONFLICT(uuid,name) DO UPDATE SET value=excluded.value")){
            ps.setString(1,uuid.toString()); ps.setString(2,name); ps.setString(3,value); ps.executeUpdate();
        } catch(Exception ignored){}
    }
    public void delGlobal(String name){
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM globals WHERE name=?")){ ps.setString(1,name); ps.executeUpdate(); } catch(Exception ignored){}
    }
    public void delPlayer(UUID uuid, String name){
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM players WHERE uuid=? AND name=?")){ ps.setString(1,uuid.toString()); ps.setString(2,name); ps.executeUpdate(); } catch(Exception ignored){}
    }
}