package ce.ajneb97.variables;

import ce.ajneb97.ConditionalEvents;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerenciador de variaveis do ConditionalEvents.
 * Substitui a dependencia externa ServerVariables.
 *
 * Suporta:
 *  - Variaveis GLOBAIS (uma por servidor) e PLAYER (uma por jogador)
 *  - ValueTypes TEXT, INTEGER, DOUBLE
 *  - possible_values (lista de valores permitidos com display custom)
 *  - limitations (min, max, max_characters, max_decimals)
 */
public class VariablesManager {

    private final ConditionalEvents plugin;
    private final Map<String, Variable> variables = new HashMap<>();
    private final Map<String, String> globalValues = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, String>> playerValues = new ConcurrentHashMap<>();
    private File dataFile;
    private boolean useSQLite = false;
    private SQLiteVariablesStorage sqlite;

    public VariablesManager(ConditionalEvents plugin) {
        this.plugin = plugin;
    }

    public void load() {
        variables.clear();
        globalValues.clear();
        playerValues.clear();

        // OTIMIZACAO: varre pasta variables/ e carrega todos os .yml
        File varDir = new File(plugin.getDataFolder(), "variables");
        if(!varDir.exists()) {
            varDir.mkdirs();
            // Copia o exemplo na primeira vez
            plugin.saveResource("variables/exemplo.yml", false);
            File example = new File(plugin.getDataFolder(), "variables/exemplo.yml");
            if(!example.exists()) {
                // fallback: tenta mover do jar manualmente
            }
        }

        File[] files = varDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if(files != null) {
            for(File ymlFile : files) {
                try {
                    FileConfiguration cfg = YamlConfiguration.loadConfiguration(ymlFile);
                    ConfigurationSection root = cfg.getConfigurationSection("variables");
                    if(root == null) continue;
                    for(String name : root.getKeys(false)) {
                        ConfigurationSection sec = root.getConfigurationSection(name);
                        if(sec == null) continue;

                        VariableType type;
                        try { type = VariableType.valueOf(sec.getString("variable_type", "GLOBAL").toUpperCase()); }
                        catch(Exception e) { type = VariableType.GLOBAL; }

                        ValueType valueType;
                        try { valueType = ValueType.valueOf(sec.getString("value_type", "TEXT").toUpperCase()); }
                        catch(Exception e) { valueType = ValueType.TEXT; }

                        Variable var = new Variable(name, type, valueType, sec.getString("initial_value", ""));
                        var.getPossibleValues().addAll(sec.getStringList("possible_values"));
                        if(sec.contains("limitations.min_value")) var.setMinValue(sec.getDouble("limitations.min_value"));
                        if(sec.contains("limitations.max_value")) var.setMaxValue(sec.getDouble("limitations.max_value"));
                        if(sec.contains("limitations.max_characters")) var.setMaxCharacters(sec.getInt("limitations.max_characters"));
                        if(sec.contains("limitations.max_decimals")) var.setMaxDecimals(sec.getInt("limitations.max_decimals"));
                        if(sec.contains("limitations.manage_out_of_range")) var.setManageOutOfRange(sec.getBoolean("limitations.manage_out_of_range"));

                        variables.put(name, var);
                    }
                } catch(Exception ex) {
                    plugin.getLogger().warning("[Variables] Falha lendo " + ymlFile.getName() + ": " + ex.getMessage());
                }
            }
        }

        useSQLite = "sqlite".equalsIgnoreCase(plugin.getConfig().getString("Variables.storage","yaml"));
        if(useSQLite){ sqlite = new SQLiteVariablesStorage(plugin); if(!sqlite.open()){ useSQLite = false; } }
        loadData();
        plugin.getLogger().info("[Variables] Carregadas " + variables.size() + " variaveis. Storage=" + (useSQLite?"sqlite":"yaml"));
    }

    private void loadData() {
        if(useSQLite){ globalValues.putAll(sqlite.loadGlobals()); playerValues.putAll(sqlite.loadPlayers()); return; }
        File dataFile = new File(plugin.getDataFolder(), "variables_data.yml");
        if(!dataFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);

        ConfigurationSection globals = cfg.getConfigurationSection("global");
        if(globals != null) {
            for(String key : globals.getKeys(false)) {
                globalValues.put(key, globals.getString(key));
            }
        }

        ConfigurationSection players = cfg.getConfigurationSection("players");
        if(players != null) {
            for(String uuidStr : players.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    ConfigurationSection pSec = players.getConfigurationSection(uuidStr);
                    if(pSec == null) continue;
                    Map<String, String> vals = new ConcurrentHashMap<>();
                    for(String varName : pSec.getKeys(false)) {
                        vals.put(varName, pSec.getString(varName));
                    }
                    playerValues.put(uuid, vals);
                } catch(Exception ignored) {}
            }
        }
    }

    public void save() {
        if(useSQLite) return;
        File dataFile = new File(plugin.getDataFolder(), "variables_data.yml");
        YamlConfiguration cfg = new YamlConfiguration();

        for(Map.Entry<String, String> e : globalValues.entrySet()) {
            cfg.set("global." + e.getKey(), e.getValue());
        }

        for(Map.Entry<UUID, Map<String, String>> e : playerValues.entrySet()) {
            String base = "players." + e.getKey().toString();
            for(Map.Entry<String, String> v : e.getValue().entrySet()) {
                cfg.set(base + "." + v.getKey(), v.getValue());
            }
        }

        try { cfg.save(dataFile); }
        catch(Exception ex) { plugin.getLogger().warning("[Variables] Falha ao salvar: " + ex.getMessage()); }
    }

    // =========================================================
    // GET
    // =========================================================
    public VariableResult get(String playerName, String variableName) {
        Variable var = variables.get(variableName);
        if(var == null) return VariableResult.error("variavel_nao_existe");

        if(playerName != null) {
            if(var.getType() == VariableType.GLOBAL) return VariableResult.error("tipo_global_esperado");
            Map<String, String> vals = playerValues.get(getUUIDByName(playerName));
            String value = vals != null ? vals.get(variableName) : null;
            return VariableResult.ok(value != null ? value : var.getInitialValue());
        } else {
            if(var.getType() == VariableType.PLAYER) return VariableResult.error("tipo_player_esperado");
            String value = globalValues.get(variableName);
            return VariableResult.ok(value != null ? value : var.getInitialValue());
        }
    }

    // =========================================================
    // SET
    // =========================================================
    public VariableResult set(String playerName, String variableName, String newValue) {
        Variable var = variables.get(variableName);
        if(var == null) return VariableResult.error("variavel_nao_existe");

        VariableResult check = validate(var, newValue);
        if(check.isError()) return check;

        String finalValue = check.getResultValue() != null ? check.getResultValue() : newValue;

        if(playerName != null) {
            if(var.getType() == VariableType.GLOBAL) return VariableResult.error("tipo_global_esperado");
            UUID uuid = getUUIDByName(playerName);
            if(uuid == null) return VariableResult.error("player_sem_dados");
            playerValues.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(variableName, finalValue);
            if(useSQLite) sqlite.putPlayer(uuid, variableName, finalValue);
        } else {
            if(var.getType() == VariableType.PLAYER) return VariableResult.error("tipo_player_esperado");
            globalValues.put(variableName, finalValue);
            if(useSQLite) sqlite.putGlobal(variableName, finalValue);
        }
        return VariableResult.ok(finalValue);
    }

    // =========================================================
    // ADD / REDUCE (numericos)
    // =========================================================
    public VariableResult add(String playerName, String variableName, String value, boolean add) {
        Variable var = variables.get(variableName);
        if(var == null) return VariableResult.error("variavel_nao_existe");
        if(!var.isNumerical()) return VariableResult.error(add ? "variavel_nao_numerica_add" : "variavel_nao_numerica_reduce");

        VariableResult current = get(playerName, variableName);
        if(current.isError()) return current;

        try {
            double oldV = Double.parseDouble(current.getResultValue());
            double delta = Double.parseDouble(value);
            double newV = add ? oldV + delta : oldV - delta;

            String newStr = var.getValueType() == ValueType.INTEGER
                    ? ((long) newV) + ""
                    : formatDouble(newV, var.getMaxDecimals());

            return set(playerName, variableName, newStr);
        } catch(NumberFormatException e) {
            return VariableResult.error("valor_invalido");
        }
    }


    /** mulvar: <nome> <valor> [player] */
    public VariableResult multiply(String playerName, String varName, String value) {
        return mathOp(playerName, varName, value, "MUL");
    }

    /** divvar: <nome> <valor> [player] */
    public VariableResult divide(String playerName, String varName, String value) {
        return mathOp(playerName, varName, value, "DIV");
    }

    /** percentvar: <nome> <porcento> [player] - ex: 10 = +10%, -5 = -5% */
    public VariableResult percent(String playerName, String varName, String value) {
        return mathOp(playerName, varName, value, "PERCENT");
    }

    private VariableResult mathOp(String playerName, String varName, String value, String op) {
        Variable var = variables.get(varName);
        if(var == null) return VariableResult.error("variavel_nao_existe");
        if(!var.isNumerical()) return VariableResult.error("variavel_nao_numerica_" + op.toLowerCase());

        VariableResult current = get(playerName, varName);
        if(current.isError()) return current;

        try {
            double oldV = Double.parseDouble(current.getResultValue());
            double arg = Double.parseDouble(value);
            double newV;
            switch(op){
                case "MUL": newV = oldV * arg; break;
                case "DIV":
                    if(arg == 0) return VariableResult.error("divisao_por_zero");
                    newV = oldV / arg;
                    break;
                case "PERCENT": newV = oldV + (oldV * arg / 100.0); break;
                default: newV = oldV;
            }

            String newStr = var.getValueType() == ValueType.INTEGER
                    ? ((long) newV) + ""
                    : formatDouble(newV, var.getMaxDecimals());
            return set(playerName, varName, newStr);
        } catch(NumberFormatException e) {
            return VariableResult.error("valor_invalido");
        }
    }

    // =========================================================
    // RESET
    // =========================================================
    public VariableResult reset(String playerName, String variableName) {
        Variable var = variables.get(variableName);
        if(var == null) return VariableResult.error("variavel_nao_existe");

        if(playerName != null) {
            if(var.getType() == VariableType.GLOBAL) return VariableResult.error("tipo_global_esperado");
            UUID uuid = getUUIDByName(playerName);
            if(uuid == null) return VariableResult.error("player_sem_dados");
            Map<String, String> vals = playerValues.get(uuid);
            if(vals != null) vals.remove(variableName);
            if(useSQLite) sqlite.delPlayer(uuid, variableName);
        } else {
            if(var.getType() == VariableType.PLAYER) return VariableResult.error("tipo_player_esperado");
            globalValues.remove(variableName);
            if(useSQLite) sqlite.putGlobal(variableName, null);
        }
        return VariableResult.ok(null);
    }

    // =========================================================
    // VALIDACAO
    // =========================================================
    private VariableResult validate(Variable var, String newValue) {
        if(!ValueType.isValid(var.getValueType(), newValue)) {
            return VariableResult.error("valor_invalido_" + var.getValueType().name());
        }

        // possible_values
        if(!var.getPossibleValues().isEmpty()) {
            boolean ok = false;
            for(String pv : var.getPossibleRealValues()) {
                if(pv.equals(newValue)) { ok = true; break; }
            }
            if(!ok) return VariableResult.error("valor_nao_permitido");
        }

        // limitations
        if(var.isNumerical()) {
            double v = Double.parseDouble(newValue);
            if(v > var.getMaxValue()) {
                if(!var.isManageOutOfRange()) return VariableResult.error("valor_acima_max");
                return VariableResult.ok(var.getValueType() == ValueType.INTEGER ? ((long)var.getMaxValue())+"" : var.getMaxValue()+"");
            }
            if(v < var.getMinValue()) {
                if(!var.isManageOutOfRange()) return VariableResult.error("valor_abaixo_min");
                return VariableResult.ok(var.getValueType() == ValueType.INTEGER ? ((long)var.getMinValue())+"" : var.getMinValue()+"");
            }
        } else {
            if(newValue.length() > var.getMaxCharacters()) return VariableResult.error("texto_muito_longo");
        }
        return VariableResult.ok(null);
    }

    private String formatDouble(double v, int decimals) {
        return new BigDecimal(v).setScale(decimals, RoundingMode.HALF_UP).toString();
    }

    // =========================================================
    // Player data
    // =========================================================
    private final Map<String, UUID> nameToUuid = new ConcurrentHashMap<>();

    public void registerPlayer(UUID uuid, String name) {
        nameToUuid.put(name, uuid);
        playerValues.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
    }

    public void unregisterPlayer(UUID uuid, String name) {
        nameToUuid.remove(name);
    }

    private UUID getUUIDByName(String name) {
        return nameToUuid.get(name);
    }

    // =========================================================
    // API publica
    // =========================================================
    public Map<String, Variable> getVariables() { return variables; }
    public Variable getVariable(String name) { return variables.get(name); }

    public VariableResult getGlobal(String varName) { return get(null, varName); }
    public VariableResult getPlayer(String playerName, String varName) { return get(playerName, varName); }
    public VariableResult setGlobal(String varName, String value) { return set(null, varName, value); }
    public VariableResult setPlayer(String playerName, String varName, String value) { return set(playerName, varName, value); }

    /** Formata o valor para exibicao usando possible_values display. */
    public String getDisplay(String playerName, String varName) {
        Variable var = variables.get(varName);
        if(var == null) return "variavel_nao_existe";
        VariableResult res = get(playerName, varName);
        if(res.isError()) return res.getErrorMessage();
        String display = var.getDisplayFromValue(res.getResultValue());
        return display != null ? display : res.getResultValue();
    }
}