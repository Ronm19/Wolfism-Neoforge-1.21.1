package com.ronm19.wolfism.entity.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.ronm19.wolfism.WolfismMod;
import com.ronm19.wolfism.entity.custom.special.VioletWolfEntity;
import net.minecraft.client.model.WolfModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Wolf;

public class VioletWolfEyesLayer extends RenderLayer<Wolf, WolfModel<Wolf>> {
    private static final ResourceLocation VIOLET_WOLF_EYES =
            ResourceLocation.fromNamespaceAndPath(WolfismMod.MOD_ID, "textures/entity/violet_wolf/violet_wolf_eyes.png");

    public VioletWolfEyesLayer(RenderLayerParent<Wolf, WolfModel<Wolf>> parent) {
        super(parent);
    }

    @Override
    public void render(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            Wolf wolf,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        if (!(wolf instanceof VioletWolfEntity violetWolf)) {
            return;
        }

        if (!violetWolf.shouldGlowEyes()) {
            return;
        }

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.eyes(VIOLET_WOLF_EYES));

        this.getParentModel().renderToBuffer(
                poseStack,
                vertexConsumer,
                15728640,
                LivingEntityRenderer.getOverlayCoords(wolf, 0.0F),
                0xFFFFFFFF
        );
    }
}