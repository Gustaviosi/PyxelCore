package ce.ajneb97;




import ce.ajneb97.configs.MainConfigManager;
import ce.ajneb97.managers.MessagesManager;
import ce.ajneb97.managers.PlayerManager;
import ce.ajneb97.managers.SavedItemsManager;
import ce.ajneb97.model.CEEvent;
import ce.ajneb97.model.EventType;
import ce.ajneb97.model.player.PlayerData;
import ce.ajneb97.utils.ActionUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class MainCommand implements CommandExecutor, TabCompleter {
	
	private ConditionalEvents plugin;
	private MainConfigManager mainConfigManager;
	public MainCommand(ConditionalEvents plugin) {
		this.plugin = plugin;
		this.mainConfigManager = plugin.getConfigsManager().getMainConfigManager();
	}
	
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
		FileConfiguration config = mainConfigManager.getConfig();
		MessagesManager msgManager = plugin.getMessagesManager();

	   	if(sender.isOp() || sender.hasPermission("conditionalevents.admin")) {
		   if(args.length >= 1) {
			   if(args[0].equalsIgnoreCase("reload")) {
				   if(!plugin.getConfigsManager().reload()){
					   sender.sendMessage(ConditionalEvents.prefix+MessagesManager.getLegacyColoredMessage(" &cThere was an error reloading the config, check the console."));
					   return true;
				   }
				   msgManager.sendMessage(sender,config.getString("Messages.commandReload"),true);
			   }else if(args[0].equalsIgnoreCase("help")) {
				   help(sender);
			   }else if(args[0].equalsIgnoreCase("reset")) {
				   reset(args,sender,config,msgManager);
			   }else if(args[0].equalsIgnoreCase("enable")) {
				   enable(args,sender,config,msgManager);
			   }else if(args[0].equalsIgnoreCase("disable")) {
				   disable(args,sender,config,msgManager);
			   }else if(args[0].equalsIgnoreCase("debug")) {
				   debug(args,sender,config,msgManager);
			   }else if(args[0].equalsIgnoreCase("verify")) {
				   if(sender instanceof Player){
					   plugin.getVerifyManager().sendVerification((Player)sender);
				   }else{
					   msgManager.sendMessage(sender,config.getString("Messages.onlyPlayerCommand"),true);
				   }
			   }else if(args[0].equalsIgnoreCase("call")) {
				   call(args,sender,config,msgManager);
			   }else if(args[0].equalsIgnoreCase("item")) {
				   if(sender instanceof Player){
					   item(args,(Player)sender,config,msgManager);
				   }else{
					   msgManager.sendMessage(sender,config.getString("Messages.onlyPlayerCommand"),true);
				   }
                    }else if(args[0].equalsIgnoreCase("interrupt")) {
                            interrupt(args,sender,config,msgManager);
                    }else if(args[0].equalsIgnoreCase("events")) {
                            if(sender instanceof Player){
                                    ce.ajneb97.gui.EventsGui.openMain((Player)sender);
                            }
                    }else if(args[0].equalsIgnoreCase("docs")) { try { java.io.File f = ce.ajneb97.utils.DocsGenerator.generate(plugin); sender.sendMessage("§8[§bPC§8] §aDocs geradas: §f"+f.getPath()); } catch(Exception ex){ sender.sendMessage("§cErro: "+ex.getMessage()); } } else if(args[0].equalsIgnoreCase("stats")) { statsCmd(sender,args); } else if(args[0].equalsIgnoreCase("export")) { exportEvent(sender,args); } else if(args[0].equalsIgnoreCase("import")) { importEvent(sender,args); } else if(args[0].equalsIgnoreCase("var")) {
                            variable(args,sender,config,msgManager);
                    }else {
                            if(sender instanceof Player){
                                    ce.ajneb97.gui.EventsGui.openMain((Player)sender);
                            }else{
                                    help(sender);
                            }
                    }
            }else {
                    if(sender instanceof Player){
                            ce.ajneb97.gui.EventsGui.openMain((Player)sender);
                    }else{
                            help(sender);
                    }
            }
        }else {
                msgManager.sendMessage(sender,config.getString("Messages.commandNoPermissions"),true);
        }

        return true;
    }

	public void reset(String[] args,CommandSender sender,FileConfiguration config,MessagesManager msgManager){
		// /ce reset <player> <event>
		if(args.length <= 2){
			msgManager.sendMessage(sender,config.getString("Messages.commandResetError"),true);
			return;
		}

		String player = args[1];
		String eventName = args[2];
		boolean silent = args.length >= 4 && args[3].equals("silent:true");

		PlayerManager playerManager = plugin.getPlayerManager();
		if(player.equals("*")){
			playerManager.resetDataForAllPlayers(eventName,config,result -> {
				if(silent){
					return;
				}
				msgManager.sendMessage(sender,result,true);
			});
		}else{
			String result = playerManager.resetDataForPlayer(player,eventName,config);
			if(silent){
				return;
			}
			msgManager.sendMessage(sender,result,true);
		}
	}
	
	public void enable(String[] args,CommandSender sender,FileConfiguration config,MessagesManager msgManager) {
		// /ce enable <event>
		if(args.length <= 1) {
			msgManager.sendMessage(sender,config.getString("Messages.eventEnableError"),true);
			return;
		}

		String eventName = args[1];
		CEEvent e = plugin.getEventsManager().getEvent(eventName);
		if(e == null) {
			msgManager.sendMessage(sender,config.getString("Messages.eventDoesNotExists"),true);
			return;
		}

		boolean silent = args.length >= 3 && args[2].equals("silent:true");
		
		e.enable();
		plugin.getConfigsManager().saveEvent(e);

		if(silent){
			return;
		}
		msgManager.sendMessage(sender,config.getString("Messages.eventEnabled")
				.replace("%event%", eventName),true);
	}
	
	public void disable(String[] args,CommandSender sender,FileConfiguration config,MessagesManager msgManager) {
		// /ce disable <event>
		if(args.length <= 1) {
			msgManager.sendMessage(sender,config.getString("Messages.eventDisableError"),true);
			return;
		}

		String eventName = args[1];
		CEEvent e = plugin.getEventsManager().getEvent(eventName);
		if(e == null) {
			msgManager.sendMessage(sender,config.getString("Messages.eventDoesNotExists"),true);
			return;
		}

		boolean silent = args.length >= 3 && args[2].equals("silent:true");

		e.disable();
		plugin.getInterruptEventManager().interruptEvent(eventName,null);

		plugin.getConfigsManager().saveEvent(e);

		if(silent){
			return;
		}
		msgManager.sendMessage(sender,config.getString("Messages.eventDisabled").replace("%event%", eventName),true);
	}

	public void debug(String[] args,CommandSender sender,FileConfiguration config,MessagesManager msgManager) {
		// /ce debug <event> (optional)<player>
		if(args.length <= 1) {
			msgManager.sendMessage(sender,config.getString("Messages.commandDebugError"),true);
			return;
		}

		String eventName = args[1];
		CEEvent e = plugin.getEventsManager().getEvent(eventName);
		if(e == null) {
			msgManager.sendMessage(sender,config.getString("Messages.eventDoesNotExists"),true);
			return;
		}

		String playerName = null;
		if(args.length >= 3){
			playerName = args[2];
		}

		boolean result = plugin.getDebugManager().setDebugSender(sender,eventName,playerName);
		if(result){
			if(playerName != null){
				msgManager.sendMessage(sender,config.getString("Messages.debugEnabledPlayer")
						.replace("%event%", eventName).replace("%player%",playerName),true);
			}else{
				msgManager.sendMessage(sender,config.getString("Messages.debugEnabled").replace("%event%", eventName),true);
			}
		}else{
			if(playerName != null){
				msgManager.sendMessage(sender,config.getString("Messages.debugDisabledPlayer")
						.replace("%event%", eventName).replace("%player%",playerName),true);
			}else{
				msgManager.sendMessage(sender,config.getString("Messages.debugDisabled").replace("%event%", eventName),true);
			}
		}
	}

	public void call(String[] args,CommandSender sender,FileConfiguration config,MessagesManager msgManager) {
		// /ce call <event> (optional)%variable1%=<value1>;%variable2%=<value2> (optional)player:<player> (optional)silent:true
		if(args.length <= 1) {
			msgManager.sendMessage(sender,config.getString("Messages.commandCallError"),true);
			return;
		}

		String eventName = args[1];
		CEEvent e = plugin.getEventsManager().getEvent(eventName);
		if(e == null) {
			msgManager.sendMessage(sender,config.getString("Messages.eventDoesNotExists"),true);
			return;
		}
		if(!e.getEventType().equals(EventType.CALL)){
			msgManager.sendMessage(sender,config.getString("Messages.commandCallInvalidEvent"),true);
			return;
		}
		if(!e.isEnabled()){
			msgManager.sendMessage(sender,config.getString("Messages.eventIsNotEnabled"),true);
			return;
		}

		String actionLine = eventName;
		String playerName = null;
		boolean silent = false;

		if(args.length >= 3){
			for(int i=2;i<args.length;i++){
				String arg = args[i];
				if(arg.contains("%")){
					actionLine += ";"+args[i];
				}else if(arg.startsWith("player:")){
					playerName = args[i].replace("player:","");
				}else if(args[i].equals("silent:true")){
					silent = true;
				}
			}
		}

		Player player = null;
		if(playerName != null){
			player = Bukkit.getPlayer(playerName);
			if(player == null){
				msgManager.sendMessage(sender,config.getString("Messages.playerNotOnline"),true);
				return;
			}
		}else if(sender instanceof Player){
			player = (Player) sender;
		}

		if(ActionUtils.callEvent(actionLine,player,plugin,null,null,null)){
			if(silent){
				return;
			}
			if(player != null){
				msgManager.sendMessage(sender,config.getString("Messages.commandCallCorrectPlayer")
						.replace("%event%",eventName).replace("%player%",player.getName()),true);
			}else{
				msgManager.sendMessage(sender,config.getString("Messages.commandCallCorrect")
						.replace("%event%",eventName),true);
			}
		}else{
			if(silent){
				return;
			}
			msgManager.sendMessage(sender,config.getString("Messages.commandCallFailed")
					.replace("%event%",eventName),true);
		}

	}

	public void item(String[] args,Player player,FileConfiguration config,MessagesManager msgManager){
        if(args.length <= 1){ ce.ajneb97.gui.ItemGui.openMain(player); return; }
        String sub = args[1].toLowerCase();

        if(sub.equals("save") || sub.equals("remove")){
            if(args.length <= 2){ msgManager.sendMessage(player,config.getString("Messages.commandItemError"),true); return; }
            SavedItemsManager savedItemsManager = plugin.getSavedItemsManager();
            String name = args[2];
            if(sub.equals("save")){
                if(savedItemsManager.getItem(name,player) != null){ msgManager.sendMessage(player,config.getString("Messages.savedItemAlreadyExists"),true); return; }
                ItemStack item = player.getItemInHand();
                if(item == null || item.getType().equals(Material.AIR)){ msgManager.sendMessage(player, config.getString("Messages.mustHaveItemInHand"), true); return; }
                savedItemsManager.addItem(name,item.clone());
                msgManager.sendMessage(player,config.getString("Messages.savedItemAdded").replace("%name%",name),true);
            }else{
                if(savedItemsManager.getItem(name,player) == null){ msgManager.sendMessage(player,config.getString("Messages.savedItemDoesNotExists"),true); return; }
                savedItemsManager.removeItem(name);
                msgManager.sendMessage(player,config.getString("Messages.savedItemRemoved").replace("%name%",name),true);
            }
            return;
        }

        if(sub.equals("name")){
            if(args.length < 3){ player.sendMessage("§7Use: §e/pc item name <nome>"); return; }
            String nome = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
            if(ce.ajneb97.utils.ItemEditor.setName(player, nome)) player.sendMessage("§7Nome §aalterado§7.");
            else player.sendMessage("§cSegure um item.");
            return;
        }

        if(sub.equals("lore")){
            if(args.length < 3){ player.sendMessage("§cUso: /ce item lore <set|add|remove|clear> [texto]"); return; }
            String op = args[2].toLowerCase();
            if(op.equals("clear")){ ce.ajneb97.utils.ItemEditor.clearLore(player); player.sendMessage("§7Lore §alimpa§7."); }
            else if(op.equals("add")){
                if(args.length < 4){ player.sendMessage("§cFalta texto."); return; }
                String txt = String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length));
                ce.ajneb97.utils.ItemEditor.addLore(player, txt);
                player.sendMessage("§aAdicionado.");
            } else if(op.equals("remove")){
                if(args.length < 4){ player.sendMessage("§cFalta index."); return; }
                try { int idx = Integer.parseInt(args[3]); if(ce.ajneb97.utils.ItemEditor.removeLore(player, idx)) player.sendMessage("§aRemovido."); else player.sendMessage("§cIndex inválido."); } catch(Exception e){ player.sendMessage("§cIndex inválido."); }
            } else if(op.equals("set")){
                if(args.length < 4){ player.sendMessage("§cFalta texto."); return; }
                String txt = String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length));
                ce.ajneb97.utils.ItemEditor.setLore(player, java.util.Arrays.asList(txt.split("\\|")));
                player.sendMessage("§aLore definida.");
            } else player.sendMessage("§cInvalido.");
            return;
        }

        if(sub.equals("pdc")){
            if(args.length < 3){ player.sendMessage("§7Use: §e/pc item pdc <set|get|remove|list>"); return; }
            String op = args[2].toLowerCase();
            if(op.equals("list")){
                java.util.Set<org.bukkit.NamespacedKey> keys = ce.ajneb97.utils.ItemEditor.listPdc(player);
                if(keys == null || keys.isEmpty()){ player.sendMessage("§eSem PDC."); return; }
                for(org.bukkit.NamespacedKey k : keys) player.sendMessage("§8- §e" + k.getKey() + "§7: §f" + ce.ajneb97.utils.ItemEditor.getPdc(player, k.getKey()));
            } else if(op.equals("set")){
                if(args.length < 4){ player.sendMessage("§cFalta chave."); return; }
                String chave = args[3];
                String valor = args.length >= 5 ? String.join(" ", java.util.Arrays.copyOfRange(args, 4, args.length)) : "";
                try { int intVal = Integer.parseInt(valor); ce.ajneb97.utils.ItemEditor.setPdcInt(player, chave, intVal); }
                catch(Exception e){ ce.ajneb97.utils.ItemEditor.setPdcString(player, chave, valor); }
                player.sendMessage("§7PDC §asetado§7: §e" + chave);
            } else if(op.equals("get")){
                if(args.length < 4){ player.sendMessage("§cFalta chave."); return; }
                String v = ce.ajneb97.utils.ItemEditor.getPdc(player, args[3]);
                player.sendMessage("§e" + args[3] + "§7: §f" + (v == null ? "(vazio)" : v));
            } else if(op.equals("remove")){
                if(args.length < 4){ player.sendMessage("§cFalta chave."); return; }
                ce.ajneb97.utils.ItemEditor.removePdc(player, args[3]);
                player.sendMessage("§aRemovido.");
            }
            return;
        }

        if(sub.equals("nbt") || sub.equals("info") || sub.equals("dump")){ player.sendMessage(ce.ajneb97.utils.ItemEditor.dump(player)); return; }

        if(sub.equals("clear")){ ce.ajneb97.utils.ItemEditor.clearItem(player); player.sendMessage("§7Item §cremovido§7."); return; }

        if(sub.equals("unbreakable")){
            boolean b = args.length >= 3 && args[2].equalsIgnoreCase("true");
            ce.ajneb97.utils.ItemEditor.setUnbreakable(player, b);
            player.sendMessage("§7Indestrutível§7: §e" + b);
            return;
        }

        if(sub.equals("food")){
            if(args.length < 4){ player.sendMessage("§7Use: §e/pc item food <nutrition> <saturation>"); return; }
            try {
                int nutrition = Integer.parseInt(args[2]);
                float sat = Float.parseFloat(args[3]);
                if(nutrition == 0 && sat == 0){ ce.ajneb97.utils.ItemEditor.clearFood(player); player.sendMessage("§aNao e mais comestivel."); }
                else { if(ce.ajneb97.utils.ItemEditor.setFood(player, nutrition, sat)) player.sendMessage("§aComestivel! n=" + nutrition + " s=" + sat); else player.sendMessage("§cFalhou (Paper 1.20.5+)."); }
            } catch(Exception e){ player.sendMessage("§cNúmeros inválidos."); }
            return;
        }

        if(sub.equals("effect")){
            if(args.length < 3){ player.sendMessage("§7Use: §e/pc item effect <add|remove|list|clear|copy>"); return; }
            String op = args[2].toLowerCase();
            ItemStack item = player.getInventory().getItemInMainHand();
            if(item == null || item.getType().isAir()){ player.sendMessage("§cSegure um item."); return; }
            if(op.equals("list")){
                java.util.List<String> list = ce.ajneb97.managers.ItemEffectManager.listEffects(item, plugin);
                if(list.isEmpty()){ player.sendMessage("§eSem efeitos."); return; }
                for(String e : list) player.sendMessage("§8- §e" + e);
            } else if(op.equals("add")){
                if(args.length < 5){ player.sendMessage("§7Use: §e/pc item effect add <potion> <duration> [amp]"); return; }
                String potion = args[3];
                int duration = Integer.parseInt(args[4]);
                int amplifier = args.length >= 6 ? Integer.parseInt(args[5]) : 0;
                if(ce.ajneb97.managers.ItemEffectManager.addEffect(plugin, item, potion, duration, amplifier)){ player.getInventory().setItemInMainHand(item); player.sendMessage("§aAdicionado: §e" + potion); }
                else player.sendMessage("§cLimite de 10.");
            } else if(op.equals("remove")){
                if(args.length < 4){ player.sendMessage("§7Use: §e/pc item effect remove <potion>"); return; }
                if(ce.ajneb97.managers.ItemEffectManager.removeEffect(item, plugin, args[3])){ player.getInventory().setItemInMainHand(item); player.sendMessage("§aRemovido."); }
                else player.sendMessage("§cNao encontrado.");
            } else if(op.equals("clear")){
                int n = ce.ajneb97.managers.ItemEffectManager.clearEffects(item, plugin);
                player.getInventory().setItemInMainHand(item);
                player.sendMessage("§7Removidos §e" + n);
            } else if(op.equals("copy")){
                if(args.length < 4){ player.sendMessage("§7Use: §e/pc item effect copy <material>"); return; }
                org.bukkit.Material srcMat = org.bukkit.Material.matchMaterial(args[3]);
                if(srcMat == null){ player.sendMessage("§cMaterial invalido."); return; }
                org.bukkit.inventory.ItemStack source = null;
                for(org.bukkit.inventory.ItemStack it : player.getInventory().getContents()){ if(it != null && it.getType() == srcMat){ source = it; break; } }
                if(source == null){ player.sendMessage("§cSem " + srcMat.name()); return; }
                int copied = 0;
                for(String eff : ce.ajneb97.managers.ItemEffectManager.listEffects(source, plugin)){
                    String[] parts = eff.split("\\|");
                    if(parts.length >= 3){ try { int dur = Integer.parseInt(parts[1]); int amp = Integer.parseInt(parts[2]); if(ce.ajneb97.managers.ItemEffectManager.addEffect(plugin, item, parts[0], dur, amp)) copied++; } catch(Exception ignored) {} }
                }
                player.getInventory().setItemInMainHand(item);
                player.sendMessage("§7Copiados §e" + copied);
            }
            return;
        }

        if(sub.equals("copy")){
            if(args.length < 3){ player.sendMessage("§7Use: §e/pc item copy <nome_salvo>"); return; }
            SavedItemsManager sim = plugin.getSavedItemsManager();
            org.bukkit.inventory.ItemStack source = sim.getItem(args[2], player);
            if(source == null){ player.sendMessage("§cNão existe."); return; }
            org.bukkit.inventory.ItemStack hand = player.getInventory().getItemInMainHand();
            if(hand == null || hand.getType().isAir()){ player.sendMessage("§cSegure um item."); return; }
            if(source.hasItemMeta()) hand.setItemMeta(source.getItemMeta());
            player.getInventory().setItemInMainHand(hand);
            player.sendMessage("§7Item §acopiado§7.");
            return;
        }

        sendItemHelp(player);
    }

    public void interrupt(String[] args,CommandSender sender,FileConfiguration config,MessagesManager msgManager) {
		// /ce interrupt <event> (optional)<player>
		if (args.length <= 1) {
			msgManager.sendMessage(sender, config.getString("Messages.commandInterruptError"), true);
			return;
		}

		String eventName = args[1];
		CEEvent e = plugin.getEventsManager().getEvent(eventName);
		if (e == null) {
			msgManager.sendMessage(sender, config.getString("Messages.eventDoesNotExists"), true);
			return;
		}

		String playerName = null;
		if (args.length >= 3 && !args[2].equals("silent:true")) {
			playerName = args[2];
		}

		boolean silent = args[args.length-1].equals("silent:true");

		plugin.getInterruptEventManager().interruptEvent(eventName,playerName);
		if(silent){
			return;
		}
		if(playerName == null){
			msgManager.sendMessage(sender, config.getString("Messages.commandInterruptCorrect")
					.replace("%event%",eventName), true);
		}else{
			msgManager.sendMessage(sender, config.getString("Messages.commandInterruptCorrectPlayer")
					.replace("%event%",eventName).replace("%player%",playerName), true);
		}
	}
	
	public void variable(String[] args, CommandSender sender, FileConfiguration config, MessagesManager msgManager) {
                if(args.length < 3){
                        sender.sendMessage("§cUse: §e/pc var <jogador> <variavel> [valor]");
                        return;
                }
                String player = args[1];
                String nome = args[2];
                ce.ajneb97.variables.VariablesManager mgr = plugin.getVariablesManager();
                if(mgr == null){ sender.sendMessage("§cVariablesManager offline."); return; }
                if(args.length == 3){
                        ce.ajneb97.variables.VariableResult r = mgr.get(player, nome);
                        if(r.isError()){ sender.sendMessage("§c" + r.getErrorMessage()); return; }
                        sender.sendMessage("§a" + player + "." + nome + " §7= §e" + r.getResultValue());
                }else{
                        StringBuilder sb = new StringBuilder();
                        for(int i = 3; i < args.length; i++){ if(i > 3) sb.append(" "); sb.append(args[i]); }
                        ce.ajneb97.variables.VariableResult r = mgr.set(player, nome, sb.toString());
                        if(r.isError()){ sender.sendMessage("§c" + r.getErrorMessage()); return; }
                        sender.sendMessage("§a" + player + "." + nome + " §7= §e" + r.getResultValue());
                }
        }

        public void exportEvent(CommandSender sender, String[] args) {
                if(args.length < 2){ sender.sendMessage("§cUse: /pc export <evento>"); return; }
                String eventName = args[1];
                ce.ajneb97.model.CEEvent e = plugin.getEventsManager().getEvent(eventName);
                if(e == null){ sender.sendMessage("§cEvento nao existe."); return; }
                java.io.File f = null;
                try { java.io.File abs = new java.io.File(e.getFilePath()); if(abs.exists()) f = abs; } catch(Exception ignored){}
                if(f == null){ java.io.File alt = new java.io.File(plugin.getDataFolder(), "events" + java.io.File.separator + e.getFilePath()); if(alt.exists()) f = alt; }
                if(f == null){ java.io.File alt2 = new java.io.File(plugin.getDataFolder(), e.getFilePath()); if(alt2.exists()) f = alt2; }
                if(f == null){ sender.sendMessage("§cArquivo do evento nao encontrado: " + e.getFilePath()); return; }
                org.bukkit.configuration.file.YamlConfiguration src = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(f);
                org.bukkit.configuration.ConfigurationSection sec = src.getConfigurationSection("Events." + eventName);
                if(sec == null){ sender.sendMessage("§cSecao Events." + eventName + " ausente no YAML."); return; }
                org.bukkit.configuration.file.YamlConfiguration out = new org.bukkit.configuration.file.YamlConfiguration();
                out.createSection("Events");
                out.set("Events." + eventName, sec);
                String yaml = out.saveToString();
                java.io.File exp = new java.io.File(plugin.getDataFolder(), "exports");
                if(!exp.exists()) exp.mkdirs();
                java.io.File outF = new java.io.File(exp, eventName + ".yml");
                try { java.nio.file.Files.write(outF.toPath(), yaml.getBytes(java.nio.charset.StandardCharsets.UTF_8)); } catch(Exception ex){ sender.sendMessage("§cErro: " + ex.getMessage()); return; }
                sender.sendMessage("§8[§bPC§8] §aExportado: §e" + eventName);
                sender.sendMessage("§8[§bPC§8] §7Arquivo: §f" + outF.getPath());
                sender.sendMessage("§8[§bPC§8] §7Pra importar: coloque conteudo em §fplugins/ConditionalEvents/imports/" + eventName + ".yml");
                for(String line : yaml.split("\n")) sender.sendMessage("§f" + line);
        }

        public void importEvent(CommandSender sender, String[] args) {
                if(args.length < 2){ sender.sendMessage("§cUse: /pc import <nome>"); return; }
                String eventName = args[1];
                if(plugin.getEventsManager().getEvent(eventName) != null){ sender.sendMessage("§cJa existe evento com esse nome."); return; }
                java.io.File imports = new java.io.File(plugin.getDataFolder(), "imports");
                if(!imports.exists()) imports.mkdirs();
                java.io.File in = new java.io.File(imports, eventName + ".yml");
                if(!in.exists()){ sender.sendMessage("§cCrie o arquivo: §f" + in.getPath()); return; }
                org.bukkit.configuration.file.YamlConfiguration cfg = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(in);
                org.bukkit.configuration.ConfigurationSection sec = cfg.getConfigurationSection("Events." + eventName);
                if(sec == null){ sender.sendMessage("§cYAML invalido (faltou Events." + eventName + ")"); return; }
                java.io.File evDir = new java.io.File(plugin.getDataFolder(), "events");
                if(!evDir.exists()) evDir.mkdirs();
                java.io.File target = new java.io.File(evDir, "imported.yml");
                org.bukkit.configuration.file.YamlConfiguration tc = target.exists() ? org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(target) : new org.bukkit.configuration.file.YamlConfiguration();
                if(!tc.isConfigurationSection("Events")) tc.createSection("Events");
                tc.set("Events." + eventName, sec);
                try { tc.save(target); } catch(Exception ex){ sender.sendMessage("§cErro: " + ex.getMessage()); return; }
                sender.sendMessage("§8[§bPC§8] §aImportado: §e" + eventName + " §7(rode §f/pc reload§7)");
        }
        public void statsCmd(CommandSender sender, String[] args) {
                ce.ajneb97.managers.StatsManager sm = plugin.getStatsManager();
                if(args.length == 1){
                        sender.sendMessage("§8[§bPC§8] §7Eventos com stats:");
                        if(sm.keys().isEmpty()){ sender.sendMessage("§7(nenhum ainda)"); return; }
                        for(String k : sm.keys()) sender.sendMessage("§8- §e"+k+" §7count=§f"+sm.count(k)+" §7avg=§f"+sm.avg(k)+"ms");
                        return;
                }
                String ev = args[1];
                if(args.length >= 3 && args[2].equalsIgnoreCase("reset")){ sm.reset(ev); sm.save(); sender.sendMessage("§8[§bPC§8] §aStats resetado: §e"+ev); return; }
                sender.sendMessage("§8[§bPC§8] §7Evento §e"+ev+"§7:");
                sender.sendMessage("§7  Disparos: §f"+sm.count(ev));
                sender.sendMessage("§7  Tempo medio: §f"+sm.avg(ev)+"ms");
                long last = sm.last(ev);
                if(last > 0) sender.sendMessage("§7  Ultimo: §f"+new java.text.SimpleDateFormat("dd/MM HH:mm:ss").format(new java.util.Date(last)));
        }
        public static void help(CommandSender sender) {
                String p = "§8[§b§lPC§8] §r";
                sender.sendMessage(p + "§7Comandos disponíveis:");
                sender.sendMessage("");
                sender.sendMessage("§b/pc §7- Abre a GUI de eventos");
                sender.sendMessage("§b/pc help §7- Mostra esta ajuda");
                sender.sendMessage("§b/pc reload §7- Recarrega as configs");
                sender.sendMessage("§b/pc verify §7- Valida todos os eventos");
                sender.sendMessage("§b/pc enable §e<evento> §7- Ativa evento");
                sender.sendMessage("§b/pc disable §e<evento> §7- Desativa evento");
                sender.sendMessage("§b/pc debug §e<evento> §7- Debuga evento");
                sender.sendMessage("§b/pc reset §e<jogador> §e<evento|all> §7- Reseta dados");
                sender.sendMessage("§b/pc call §e<evento> §7- Chama um evento CALL");
                sender.sendMessage("§b/pc interrupt §e<evento> §7- Interrompe execução");
                sender.sendMessage("§b/pc item §7- Editor de item (GUI)");
                sender.sendMessage("§b/pc events §7- Lista de eventos (GUI)");
                sender.sendMessage("§b/pc var §e<jogador> §e<nome> §7- Vê/edita variável");
        }

	@Override
        public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
                if(!(sender.isOp() || sender.hasPermission("conditionalevents.admin"))) return null;
                if(args.length == 1){
                        List<String> sub = new ArrayList<String>();
                        String[] all = {"help","reload","verify","reset","debug","enable","disable","call","item","interrupt","events","var","export","import","stats","docs"};
                        for(String c : all) if(args[0].isEmpty() || c.startsWith(args[0].toLowerCase())) sub.add(c);
                        return sub;
                }
                String s0 = args[0].toLowerCase();
                if(s0.equals("debug") && args.length == 2) return getEventsCompletions(args,1,false,null);
                if(s0.equals("debug") && args.length == 3) return getPlayerCompletions(args,2,false);
                if((s0.equals("enable") || s0.equals("disable")) && args.length == 2) return getEventsCompletions(args,1,false,null);
                if((s0.equals("enable") || s0.equals("disable")) && args.length == 3) return filterOne("silent:true", args[2]);
                if(s0.equals("interrupt")){
                        if(args.length == 2) return getEventsCompletions(args,1,false,null);
                        if(args.length == 3){ List<String> pcs = getPlayerCompletions(args,2,false); if(pcs == null) pcs = new ArrayList<String>(); if(args[2].isEmpty() || "silent:true".startsWith(args[2].toLowerCase())) pcs.add("silent:true"); return pcs.isEmpty() ? null : pcs; }
                        if(args.length == 4) return filterOne("silent:true", args[3]);
                }
                if(s0.equals("call")){
                        if(args.length == 2) return getEventsCompletions(args,1,false,EventType.CALL);
                        if(args.length >= 3){ String last = args[args.length-1]; List<String> cc = new ArrayList<String>(); if(last.isEmpty() || "player:".startsWith(last.toLowerCase())) cc.add("player:"); if(last.isEmpty() || "silent:true".startsWith(last.toLowerCase())) cc.add("silent:true"); return cc.isEmpty() ? null : cc; }
                }
                if(s0.equals("reset")){
                        if(args.length == 2) return getPlayerCompletions(args,1,true);
                        if(args.length == 3) return getEventsCompletions(args,2,true,null);
                        if(args.length == 4) return filterOne("silent:true", args[3]);
                }
                if(s0.equals("item")){
                        if(args.length == 2){ List<String> ic = new ArrayList<String>(); String[] subs = {"save","remove","name","lore","pdc","nbt","info","dump","clear","unbreakable","food","effect","copy"}; for(String x : subs) if(args[1].isEmpty() || x.startsWith(args[1].toLowerCase())) ic.add(x); return ic; }
                        if(args.length == 3){ String s1 = args[1].toLowerCase(); if(s1.equals("save") || s1.equals("remove") || s1.equals("copy")) return getSavedItemsCompletions(args,2); if(s1.equals("lore")) return filterArr(new String[]{"set","add","remove","clear"}, args[2]); if(s1.equals("pdc")) return filterArr(new String[]{"set","get","remove","list"}, args[2]); if(s1.equals("effect")) return filterArr(new String[]{"add","remove","list","clear","copy"}, args[2]); if(s1.equals("unbreakable")) return filterArr(new String[]{"true","false"}, args[2]); }
                }
                if(s0.equals("var") && args.length == 2) return getPlayerCompletions(args,1,false);
                if(s0.equals("stats") && args.length == 2) return getEventsCompletions(args,1,false,null);
                if(s0.equals("export") && args.length == 2) return getEventsCompletions(args,1,false,null);
                return null;
        }

        private List<String> filterOne(String val, String arg){ if(arg.isEmpty() || val.toLowerCase().startsWith(arg.toLowerCase())){ List<String> c = new ArrayList<String>(); c.add(val); return c; } return null; }

        private List<String> filterArr(String[] vals, String arg){ List<String> c = new ArrayList<String>(); for(String v : vals) if(arg.isEmpty() || v.toLowerCase().startsWith(arg.toLowerCase())) c.add(v); return c.isEmpty() ? null : c; }

        private List<String> getPlayerCompletions(String[] args, int pos, boolean addAll){ List<String> c = new ArrayList<String>(); String aa = args[pos]; for(Player pp : Bukkit.getOnlinePlayers()) if(aa.isEmpty() || pp.getName().toLowerCase().startsWith(aa.toLowerCase())) c.add(pp.getName()); if(addAll && (aa.isEmpty() || "*".startsWith(aa))) c.add("*"); return c.isEmpty() ? null : c; }
public List<String> getEventsCompletions(String[] args,int argEventPos,boolean addAll,EventType eventType){
		List<String> completions = new ArrayList<String>();

		String argEvent = args[argEventPos];

		if(addAll) {
			if(argEvent.isEmpty() || "all".startsWith(argEvent.toLowerCase())) {
				completions.add("all");
			}
		}

		ArrayList<CEEvent> events = plugin.getEventsManager().getEvents();
		for(CEEvent event : events) {
			if(argEvent.isEmpty() || event.getName().toLowerCase().startsWith(argEvent.toLowerCase())) {
				if(eventType != null){
					if(event.getEventType().equals(eventType)){
						completions.add(event.getName());
					}
				}else{
					completions.add(event.getName());
				}
			}
		}

		if(completions.isEmpty()){
			return null;
		}
		return completions;
	}

	public List<String> getSavedItemsCompletions(String[] args,int argItemPos){
		List<String> completions = new ArrayList<>();

		String argItem = args[argItemPos];
		for(Map.Entry<String, ItemStack> entry : plugin.getSavedItemsManager().getSavedItems().entrySet()){
			if(argItem.isEmpty() || entry.getKey().toLowerCase().startsWith(argItem.toLowerCase())) {
				completions.add(entry.getKey());
			}
		}

		if(completions.isEmpty()){
			return null;
		}
		return completions;
	}

        

        private void sendItemHelp(org.bukkit.entity.Player player){
                player.sendMessage("§8[§bCE Item§8] §7Subcomandos:");
                player.sendMessage("§e/ce item §7- abre GUI");
                player.sendMessage("§e/ce item save|remove <nome>");
                player.sendMessage("§e/ce item name <texto>");
                player.sendMessage("§e/ce item lore <set|add|remove|clear>");
                player.sendMessage("§e/ce item pdc <set|get|remove|list>");
                player.sendMessage("§e/ce item food <nutrition> <saturation>");
                player.sendMessage("§e/ce item effect <add|remove|list|clear|copy>");
                player.sendMessage("§e/ce item nbt §7- dump do item");
                player.sendMessage("§e/ce item unbreakable <true|false>");
                player.sendMessage("§e/ce item copy <nome_salvo>");
                player.sendMessage("§e/ce item clear");
        }
}
