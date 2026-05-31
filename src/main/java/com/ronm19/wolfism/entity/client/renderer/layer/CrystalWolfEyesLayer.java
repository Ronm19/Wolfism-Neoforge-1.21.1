package com.ronm19.wolfism.entity.client.renderer.layer;

// keep your real package line here

import com.mojang.blaze3d.vertex.PoseStack;
import com.ronm19.wolfism.WolfismMod;
import com.ronm19.wolfism.entity.custom.special.CrystalWolfEntity;
import net.minecraft.client.model.WolfModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Wolf;

public class CrystalWolfEyesLayer extends EyesLayer<Wolf, WolfModel<Wolf>> {

    private static final RenderType CRYSTAL_WOLF_EYES = RenderType.eyes(
            ResourceLocation.fromNamespaceAndPath(WolfismMod.MOD_ID, "textures/entity/crystal_wolf/crystal_wolf_eyes.png"));

    public CrystalWolfEyesLayer(RenderLayerParent<Wolf, WolfModel<Wolf>> parent) {
        super(parent);
    }

    @Override
    public RenderType renderType() {
        return CRYSTAL_WOLF_EYES;
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

        if (!(wolf instanceof CrystalWolfEntity crystalWolf)) {
            return;
        }

        if (!crystalWolf.shouldEyesGlow()) {
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