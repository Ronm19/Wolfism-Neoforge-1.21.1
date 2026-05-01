package com.ronm19.wolfism.entity.client.renderer;

import com.ronm19.wolfism.WolfismMod;
import com.ronm19.wolfism.entity.client.renderer.layer.StormWolfEyesLayer;
import com.ronm19.wolfism.entity.custom.elemental.StormWolfEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Wolf;
import org.jetbrains.annotations.NotNull;

public class StormWolfRenderer extends WolfRenderer {

    public static final ResourceLocation NORMAL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(WolfismMod.MOD_ID, "textures/entity/storm_wolf/storm_wolf.png");

    public static final ResourceLocation CHARGED_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(WolfismMod.MOD_ID, "textures/entity/storm_wolf/storm_wolf_charged.png");

    public StormWolfRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.addLayer(new StormWolfEyesLayer(this));
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation( @NotNull Wolf entity) {
        if (entity instanceof StormWolfEntity stormWolf && stormWolf.isCharged()) {
            return CHARGED_TEXTURE;
        }

        return NORMAL_TEXTURE;
    }
}