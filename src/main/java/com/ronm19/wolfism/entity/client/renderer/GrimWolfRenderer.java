package com.ronm19.wolfism.entity.client.renderer;

import com.ronm19.wolfism.WolfismMod;
import com.ronm19.wolfism.entity.client.renderer.layer.GrimWolfEyesLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Wolf;
import org.jetbrains.annotations.NotNull;

public class GrimWolfRenderer extends WolfRenderer {

    private static final ResourceLocation GRIM_WOLF_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(WolfismMod.MOD_ID, "textures/entity/grim_wolf/grim_wolf.png");

    public GrimWolfRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.addLayer(new GrimWolfEyesLayer(this));
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation( Wolf wolf) {
        return GRIM_WOLF_TEXTURE;
    }
}