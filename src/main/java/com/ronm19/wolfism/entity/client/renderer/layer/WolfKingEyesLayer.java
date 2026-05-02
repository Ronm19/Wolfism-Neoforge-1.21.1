package com.ronm19.wolfism.entity.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.ronm19.wolfism.WolfismMod;
import com.ronm19.wolfism.entity.custom.special.WolfKingEntity;
import net.minecraft.client.model.WolfModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Wolf;

public class WolfKingEyesLayer extends RenderLayer<Wolf, WolfModel<Wolf>> {
    private static final ResourceLocation EYES_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(WolfismMod.MOD_ID, "textures/entity/wolf_king/wolf_king_eyes.png");

    public WolfKingEyesLayer(RenderLayerParent<Wolf, WolfModel<Wolf>> parent) {
        super(parent);
    }

    @Override
    public void render(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            Wolf entity,
            float limbSwing,
            float limbSwingAmount,
            float partialTick,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        if (!(entity instanceof WolfKingEntity wolfKing)) {
            return;
        }

        if (!wolfKing.shouldGlowEyes()) {
            return;
        }

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.eyes(EYES_TEXTURE));

        this.getParentModel().renderToBuffer(
                poseStack,
                vertexConsumer,
                LightTexture.FULL_BRIGHT,
                LivingEntityRenderer.getOverlayCoords(entity, 0.0F),
                0xFFFFFFFF
        );
    }
}