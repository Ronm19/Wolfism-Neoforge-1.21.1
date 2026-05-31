package com.ronm19.wolfism.entity.client.renderer;

import com.ronm19.wolfism.WolfismMod;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Wolf;
import org.jetbrains.annotations.NotNull;

public class ZombieWolfRenderer extends WolfRenderer {

    private static final ResourceLocation ZOMBIE_WOLF_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(WolfismMod.MOD_ID, "textures/entity/zombie_wolf/zombie_wolf.png");

    public ZombieWolfRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation( @NotNull Wolf wolf) {
        return ZOMBIE_WOLF_TEXTURE;
    }
}