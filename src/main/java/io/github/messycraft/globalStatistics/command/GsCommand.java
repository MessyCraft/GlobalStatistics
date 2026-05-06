package io.github.messycraft.globalStatistics.command;

import io.github.messycraft.globalStatistics.api.StatisticEntry;
import io.github.messycraft.globalStatistics.service.StatisticsService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class GsCommand implements CommandExecutor, TabCompleter {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final JavaPlugin plugin;
    private final StatisticsService service;

    public GsCommand(JavaPlugin plugin, StatisticsService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String[] args) {
        if (args.length == 0) {
            send(sender, "&6GlobalStatistics &7- 可用子命令：&6create&7/&6delete&7/&6export&7/&6list&7/&6record&7/&6query");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "create":
                return handleCreate(sender, args);
            case "delete":
                return handleDelete(sender, args);
            case "export":
                return handleExport(sender, args);
            case "list":
                return handleList(sender);
            case "record":
                return handleRecord(sender, args);
            case "query":
                return handleQuery(sender, args);
            default:
                send(sender, "&c未知子命令：&6" + sub + "&c");
                send(sender, "&6用法：&7/gs <create|delete|export|list|record|query>");
                return true;
        }
    }

    private boolean handleCreate(CommandSender sender, String[] args) {
        if (args.length != 2) {
            send(sender, "&c参数不足. &7正确用法：&6/gs create <key>");
            return true;
        }

        String key = args[1];
        send(sender, "&6正在创建数据集：&e" + key + "&6，请稍候...");
        executeAsync(sender, new Task() {
            @Override
            public void run() throws Exception {
                boolean created = service.createDataset(key);
                if (created) {
                    sendSync(sender, "&6创建成功：&e" + key + "&7. 你现在可以使用 &6/gs record " + key + " <content...> &7进行记录");
                } else {
                    sendSync(sender, "&c创建失败：数据集已存在 -> &e" + key + "&c");
                }
            }
        });
        return true;
    }

    private boolean handleDelete(CommandSender sender, String[] args) {
        if (args.length != 2) {
            send(sender, "&c参数不足. &7正确用法：&6/gs delete <key>");
            return true;
        }

        String key = args[1];
        send(sender, "&6正在删除数据集：&e" + key + "&6，请稍候...");
        executeAsync(sender, new Task() {
            @Override
            public void run() throws Exception {
                boolean deleted = service.deleteDataset(key);
                if (deleted) {
                    sendSync(sender, "&6删除成功：&e" + key + "&7");
                } else {
                    sendSync(sender, "&c删除失败：未找到数据集 -> &e" + key + "&c");
                }
            }
        });
        return true;
    }

    private boolean handleExport(CommandSender sender, String[] args) {
        if (args.length == 1) {
            send(sender, "&6正在导出全部数据集到 CSV，这可能需要一点时间...");
            executeAsync(sender, new Task() {
                @Override
                public void run() throws Exception {
                    Map<String, File> files = service.exportAllDatasets();
                    sendSync(sender, "&6导出完成，共处理 &e" + files.size() + " &6个数据集：");
                    for (Map.Entry<String, File> entry : files.entrySet()) {
                        sendSync(sender, "&6- &e" + entry.getKey() + " &7-> &6" + entry.getValue().getAbsolutePath());
                    }
                }
            });
            return true;
        }

        if (args.length == 2) {
            String key = args[1];
            send(sender, "&6正在导出数据集：&e" + key + "&6，请稍候...");
            executeAsync(sender, new Task() {
                @Override
                public void run() throws Exception {
                    File file = service.exportDataset(key);
                    if (file == null) {
                        sendSync(sender, "&c导出失败：未找到数据集 -> &e" + key + "&c");
                        return;
                    }
                    sendSync(sender, "&6导出成功：&e" + key + " &7-> &6" + file.getAbsolutePath());
                }
            });
            return true;
        }

        send(sender, "&c参数错误. &7正确用法：&6/gs export [key]");
        return true;
    }

    private boolean handleList(CommandSender sender) {
        send(sender, "&6正在读取数据集列表...");
        executeAsync(sender, new Task() {
            @Override
            public void run() throws Exception {
                List<String> datasets = service.listDatasets();
                sendSync(sender, "&6当前数据集数量：&e" + datasets.size());
                for (String dataset : datasets) {
                    sendSync(sender, "&6- &e" + dataset);
                }
            }
        });
        return true;
    }

    private boolean handleRecord(CommandSender sender, String[] args) {
        if (args.length < 3) {
            send(sender, "&c参数不足. &7正确用法：&6/gs record <key> <content...>");
            return true;
        }

        final String key = args[1];
        final String content = joinFrom(args, 2);
        send(sender, "&6正在写入记录到：&e" + key + "&6，请稍候...");
        executeAsync(sender, new Task() {
            @Override
            public void run() throws Exception {
                boolean success = service.record(key, content);
                if (success) {
                    sendSync(sender, "&6记录成功：&e" + key + "&7 <- &6" + content);
                } else {
                    sendSync(sender, "&c记录失败：未找到数据集 -> &e" + key + "&c");
                }
            }
        });
        return true;
    }

    private boolean handleQuery(CommandSender sender, String[] args) {
        if (args.length < 2) {
            send(sender, "&c参数不足. &7正确用法：&6/gs query <key> [all | yyyy m d | startTs endTs]");
            return true;
        }

        if (args.length != 2 && args.length != 3 && args.length != 4 && args.length != 5) {
            send(sender, "&c参数错误. &7正确用法：&6/gs query <key> [all | yyyy m d | startTs endTs]");
            return true;
        }

        final String key = args[1];
        send(sender, "&6正在查询数据集：&e" + key + "&6，请稍候...");
        executeAsync(sender, new Task() {
            @Override
            public void run() throws Exception {
                if (!service.datasetExists(key)) {
                    sendSync(sender, "&c查询失败：未找到数据集 -> &e" + key + "&c");
                    return;
                }

                List<StatisticEntry> entries;
                if (args.length == 2) {
                    LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
                    entries = service.queryByDate(key, now.getYear(), now.getMonthValue(), now.getDayOfMonth());
                } else if (args.length == 3 && "all".equalsIgnoreCase(args[2])) {
                    entries = service.queryAll(key);
                } else if (args.length == 5) {
                    int year = Integer.parseInt(args[2]);
                    int month = Integer.parseInt(args[3]);
                    int day = Integer.parseInt(args[4]);
                    entries = service.queryByDate(key, year, month, day);
                } else if (args.length == 4) {
                    long start = Long.parseLong(args[2]);
                    long end = Long.parseLong(args[3]);
                    entries = service.queryByRange(key, start, end);
                } else {
                    sendSync(sender, "&c参数错误. &7正确用法：&6/gs query <key> [all | yyyy m d | startTs endTs]");
                    return;
                }

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    send(sender, "&6查询完成：&e" + key + "&6，共找到 &e" + entries.size() + " &6条记录");
                    int i = 0;
                    for (StatisticEntry entry : entries) {
                        send(sender, "&6" + (++i) + ". &7" + formatTimestamp(entry.timestamp()) + " &6| &e" + entry.content());
                    }
                });
            }
        });
        return true;
    }

    @Override
    public List<String> onTabComplete(@NonNull CommandSender sender, @NonNull Command command, @NonNull String alias, String[] args) {
        if (args.length == 1) {
            return filterByPrefix(Arrays.asList("create", "delete", "export", "list", "record", "query"), args[0]);
        }

        if (args.length == 2 && ("delete".equalsIgnoreCase(args[0])
                || "record".equalsIgnoreCase(args[0])
                || "query".equalsIgnoreCase(args[0])
                || "export".equalsIgnoreCase(args[0]))) {
            try {
                return filterByPrefix(service.listDatasets(), args[1]);
            } catch (SQLException ignored) {
                return Collections.emptyList();
            }
        }

        return Collections.emptyList();
    }

    private String joinFrom(String[] args, int start) {
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (i > start) {
                builder.append(' ');
            }
            builder.append(args[i]);
        }
        return builder.toString();
    }

    private String formatTimestamp(long ts) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(ts), ZoneId.systemDefault()).format(TIME_FORMATTER)
                + "(" + ts + ")";
    }

    private List<String> filterByPrefix(List<String> source, String prefix) {
        List<String> matches = new ArrayList<String>();
        for (String value : source) {
            if (value.toLowerCase().startsWith(prefix.toLowerCase())) {
                matches.add(value);
            }
        }
        return matches;
    }

    private void executeAsync(final CommandSender sender, final Task task) {
        CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    task.run();
                } catch (NumberFormatException e) {
                    sendSync(sender, "&c参数格式错误：请输入有效数字（日期或时间戳）");
                } catch (SQLException e) {
                    sendSync(sender, "&c数据库操作失败：&7" + e.getMessage());
                } catch (Exception e) {
                    sendSync(sender, "&c操作失败：&7" + e.getMessage());
                }
            }
        });
    }

    private void send(CommandSender sender, String message) {
        sender.sendMessage(color(message));
    }

    private void sendSync(final CommandSender sender, final String message) {
        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                sender.sendMessage(color(message));
            }
        });
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private interface Task {
        void run() throws Exception;
    }
}
