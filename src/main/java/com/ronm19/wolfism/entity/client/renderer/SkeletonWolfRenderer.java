package com.ronm19.wolfism.entity.client.renderer;

import com.ronm19.wolfism.WolfismMod;
import com.ronm19.wolfism.entity.client.renderer.layer.SkeletonWolfEyesLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Wolf;
import org.jetbrains.annotations.NotNull;

public class SkeletonWolfRenderer extends WolfRenderer {

    private static final ResourceLocation SKELETON_WOLF_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(WolfismMod.MOD_ID, "textures/entity/skeleton_wolf/skeleton_wolf.png");

    public SkeletonWolfRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.addLayer(new SkeletonWolfEyesLayer(this));
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation( Wolf wolf) {
        return SKELETON_WOLF_TEXTURE;
    }
}