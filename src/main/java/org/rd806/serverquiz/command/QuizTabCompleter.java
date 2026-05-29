package org.rd806.serverquiz.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class QuizTabCompleter implements TabCompleter {
    @Override
    public List<String> onTabComplete(@NonNull CommandSender sender, @NonNull Command command, @NonNull String alias, String @NonNull [] args) {
        List<String> completions = new ArrayList<>();

        if (command.getName().equalsIgnoreCase("serverquiz") || command.getName().equalsIgnoreCase("quiz")) {
            if (args.length == 1) {
                AddCompletions(completions, "initial", "send", "open", "show", "edit", "reload");
                return completions;
            } else if (args.length == 2) {
                switch (args[0]) {
                    case "send":
                        AddCompletions(completions, "<question ID>");
                        return completions;
                    case "show":
                        AddCompletions(completions, "num", "interval", "storage", "info", "score");
                        return completions;
                    case "edit":
                        AddCompletions(completions, "interval", "storage", "choice", "blank");
                        return completions;
                }
            }  else if (args.length == 3) {
                switch (args[1]) {
                    case "info":
                        AddCompletions(completions, "<quiz ID>");
                    case "score":
                        AddCompletions(completions, "<target Player>");
                    case "choice", "blank":
                        AddCompletions(completions, "insert", "alter", "delete");
                        return completions;
                }
            } else if (args.length >= 4) {
                if (args[1].equals("choice")) {
                    switch (args[2]) {
                        case "insert":
                            AddCompletions(completions, "<question> <optionA> <optionB> <optionC> <optionD> <answer> <reward>");
                            return completions;
                        case "alter":
                        case "delete":
                    }
                } else if (args[1].equals("blank")) {
                    switch (args[2]) {
                        case "insert":
                            AddCompletions(completions, "<question> <answer> <reward>");
                            return completions;
                        case "alter":
                        case "delete":
                    }
                }
            }
        }
        return null;
    }

    private void AddCompletions(List<String> completions, String... strings) {
        // 清除原来的内容
        completions.clear();
        if (strings != null && strings.length > 0) {
            completions.addAll(Arrays.asList(strings));
        }
    }
}
