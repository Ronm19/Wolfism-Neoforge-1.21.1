package com.ronm19.wolfism.entity.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ronm19.wolfism.WolfismMod;
import com.ronm19.wolfism.entity.custom.special.DrownedWolfEntity;
import net.minecraft.client.model.WolfModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Wolf;


public class DrownedWolfEyesLayer extends EyesLayer<Wolf, WolfModel<Wolf>> {

    private static final RenderType DROWNED_WOLF_EYES = RenderType.eyes(
            ResourceLocation.fromNamespaceAndPath(WolfismMod.MOD_ID, "textures/entity/drowned_wolf/drowned_wolf_eyes.png"));

    public DrownedWolfEyesLayer(RenderLayerParent<Wolf, WolfModel<Wolf>> parent) {
        super(parent);
    }

    @Override
    public RenderType renderType() {
        return DROWNED_WOLF_EYES;
    }

    @Override
    public void render(PoseStack poseStack,
                       MultiBufferSource buffer,
                       int packedLight,
                       Wolf wolf,
                       float limbSwing,
                       float limbSwingAmount,
                       float partialTick,
                       float ageInTicks,
                       float netHeadYaw,
                       float headPitch) {

        if (!(wolf instanceof DrownedWolfEntity drownedWolf)) {
            return;
        }

        if (!drownedWolf.shouldEyesGlow()) {
            return;
        }

        super.render(
                poseStack,
                buffer,
                packedLight,
                wolf,
                limbSwing,
                limbSwingAmount,
                partialTick,
                ageInTicks,
                netHeadYaw,
                headPitch
        );
    }
}