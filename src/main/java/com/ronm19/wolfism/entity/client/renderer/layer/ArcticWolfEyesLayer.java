package com.ronm19.wolfism.entity.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.ronm19.wolfism.entity.custom.neutral.ArcticWolfEntity;
import net.minecraft.client.model.WolfModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Wolf;
import org.jetbrains.annotations.NotNull;

public class ArcticWolfEyesLayer extends RenderLayer<Wolf, WolfModel<Wolf>> {

    private static final ResourceLocation EYES =
            ResourceLocation.fromNamespaceAndPath("wolfism", "textures/entity/arctic_wolf/arctic_wolf_eyes.png");

    public ArcticWolfEyesLayer(RenderLayerParent<Wolf, WolfModel<Wolf>> parent) {
        super(parent);
    }

    @Override
    public void render(@NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight,
                       @NotNull Wolf wolf, float limbSwing, float limbSwingAmount,
                       float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {

        if (!(wolf instanceof ArcticWolfEntity arctic)) return;

        if (shouldGlow(arctic)) {
            VertexConsumer vertexconsumer = buffer.getBuffer(RenderType.eyes(EYES));

            this.getParentModel().renderToBuffer(
                    poseStack,
                    vertexconsumer,
                    15728640,
                    LivingEntityRenderer.getOverlayCoords(wolf, 0.0F),
                    0xFFFFFFFF
            );
        }
    }

    private boolean shouldGlow(ArcticWolfEntity wolf) {
        long timeOfDay = wolf.level().getDayTime() % 24000L;
        return timeOfDay >= 13000L && timeOfDay <= 23000L;
    }
}