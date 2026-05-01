package com.ronm19.wolfism.entity.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ronm19.wolfism.WolfismMod;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Wolf;

public class EarthWolfRenderer extends WolfRenderer {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(WolfismMod.MOD_ID, "textures/entity/earth_wolf/earth_wolf.png");

    public EarthWolfRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(net.minecraft.world.entity.animal.Wolf entity) {
        return TEXTURE;
    }

    @Override
    public void render( Wolf entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight ) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
}