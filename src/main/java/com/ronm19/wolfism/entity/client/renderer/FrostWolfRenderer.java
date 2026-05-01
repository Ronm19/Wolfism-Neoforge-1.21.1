package com.ronm19.wolfism.entity.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ronm19.wolfism.WolfismMod;
import com.ronm19.wolfism.entity.client.renderer.layer.FrostWolfEyesLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Wolf;
import org.jetbrains.annotations.NotNull;

public class FrostWolfRenderer extends WolfRenderer {

    private static final ResourceLocation FROST_WOLF_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(WolfismMod.MOD_ID, "textures/entity/frost_wolf/frost_wolf.png");

    public FrostWolfRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.addLayer(new FrostWolfEyesLayer(this));
    }

    public @NotNull ResourceLocation getTextureLocation( @NotNull Wolf entity) {
        return FROST_WOLF_TEXTURE;
    }

    @Override
    protected void scale( @NotNull Wolf entity, PoseStack poseStack, float partialTickTime) {
        poseStack.scale(1.12F, 1.12F, 1.12F);
        super.scale(entity, poseStack, partialTickTime);
    }
}