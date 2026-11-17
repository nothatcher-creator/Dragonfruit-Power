package com.nothatcher.dragonfruit;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.nothatcher.dragonfruit.power.PowerManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** /dragonfruit clear  |  /dragonfruit set <rubber|flame|ice|light|quake>  (op level 2) */
public final class ModCommands {
    private ModCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("dragonfruit")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(CommandManager.literal("clear").executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                            PowerManager.clear(p);
                            ctx.getSource().sendFeedback(() -> Text.literal("Your Dragonfruit power is gone."), false);
                            return 1;
                        }))
                        .then(CommandManager.literal("set")
                                .then(CommandManager.argument("fruit", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            for (FruitType t : FruitType.values()) {
                                                if (t != FruitType.NONE) builder.suggest(t.id);
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                                            FruitType t = FruitType.byId(StringArgumentType.getString(ctx, "fruit"));
                                            if (t == FruitType.NONE) {
                                                ctx.getSource().sendError(Text.literal("Unknown fruit."));
                                                return 0;
                                            }
                                            PowerManager.grant(p, t);
                                            return 1;
                                        })))));
    }
}
