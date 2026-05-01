package com.ronm19.wolfism.entity.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.ronm19.wolfism.WolfismMod;
import com.ronm19.wolfism.entity.custom.special.SpiritWolfEntity;
import net.minecraft.client.model.WolfModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Wolf;

public class SpiritWolfEyesLayer extends RenderLayer<Wolf, WolfModel<Wolf>> {

    private static final ResourceLocation SPIRIT_WOLF_EYES =
            ResourceLocation.fromNamespaceAndPath(
                    WolfismMod.MOD_ID,
                    "textures/entity/spirit_wolf/spirit_wolf_eyes.png"
            );

    private static final RenderType SPIRIT_WOLF_EYES_RENDER_TYPE =
            RenderType.eyes(SPIRIT_WOLF_EYES);

    public SpiritWolfEyesLayer(RenderLayerParent<Wolf, WolfModel<Wolf>> parent) {
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
            float partialTick,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        if (!(wolf instanceof SpiritWolfEntity spiritWolf)) {
            return;
        }

        if (!spiritWolf.shouldGlowEyes()) {
            return;
        }

        float intensity = 0.55F;

        if (spiritWolf.isSpiritVeilActive()) {
            float pulse = (Mth.sin((ageInTicks + partialTick) * 0.4F) + 1.0F) * 0.5F;
            intensity = 0.7F + pulse * 0.3F;
        }

        VertexConsumer vertexConsumer = buffer.getBuffer(SPIRIT_WOLF_EYES_RENDER_TYPE);

        int color = FastColor.ARGB32.colorFromFloat(
                1.0F,
                intensity,
                intensity,
                intensity
        );

        this.getParentModel().renderToBuffer(
                poseStack,
                vertexConsumer,
                15728640,
                OverlayTexture.NO_OVERLAY,
                color
        );
    }
}