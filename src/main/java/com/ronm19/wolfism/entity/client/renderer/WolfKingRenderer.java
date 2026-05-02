package com.ronm19.wolfism.entity.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ronm19.wolfism.WolfismMod;
import com.ronm19.wolfism.entity.client.renderer.layer.WolfKingEyesLayer;
import com.ronm19.wolfism.entity.custom.special.WolfKingEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Wolf;

public class WolfKingRenderer extends WolfRenderer {
    private static final ResourceLocation NORMAL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(WolfismMod.MOD_ID, "textures/entity/wolf_king/wolf_king.png");

    private static final ResourceLocation ROYAL_RAGE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(WolfismMod.MOD_ID, "textures/entity/wolf_king/wolf_king_royal_rage.png");

    public WolfKingRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.addLayer(new WolfKingEyesLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(Wolf entity) {
        if (entity instanceof WolfKingEntity wolfKing && wolfKing.isRoyalRageActive()) {
            return ROYAL_RAGE_TEXTURE;
        }

        return NORMAL_TEXTURE;
    }

    @Override
    protected void scale(Wolf entity, PoseStack poseStack, float partialTickTime) {
        poseStack.scale(1.18F, 1.18F, 1.18F);
        super.scale(entity, poseStack, partialTickTime);
    }
}