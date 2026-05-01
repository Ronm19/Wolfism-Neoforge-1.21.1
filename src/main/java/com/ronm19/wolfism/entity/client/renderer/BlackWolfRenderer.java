package com.ronm19.wolfism.entity.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ronm19.wolfism.WolfismMod;
import com.ronm19.wolfism.entity.client.renderer.layer.BlackWolfEyesLayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Wolf;
import org.jetbrains.annotations.NotNull;

public class BlackWolfRenderer extends WolfRenderer {

    private static final ResourceLocation BLACK_WOLF_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(WolfismMod.MOD_ID, "textures/entity/black_wolf/black_wolf.png");

    public BlackWolfRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.addLayer(new BlackWolfEyesLayer(this));
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation( net.minecraft.world.entity.animal.@NotNull Wolf entity) {
        return BLACK_WOLF_TEXTURE;
    }

    @Override
    public void render( Wolf entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight ) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
}