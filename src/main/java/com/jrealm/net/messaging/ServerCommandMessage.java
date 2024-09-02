package com.jrealm.net.messaging;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServerCommandMessage {
    private String command;
    private List<String> args;

    public static ServerCommandMessage parseFromInput(String line) {
        final String command = line.substring(line.indexOf("/") + 1);
        final String[] commandArgs = command.split(" ");
        final List<String> argsList = Stream.of(Arrays.copyOfRange(commandArgs, 1, commandArgs.length))
                .collect(Collectors.toList());
        return ServerCommandMessage.builder().command(commandArgs[0]).args(argsList).build();
    }
}
