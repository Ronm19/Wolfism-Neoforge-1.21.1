package com.ronm19.wolfism.entity.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ronm19.wolfism.WolfismMod;
import com.ronm19.wolfism.entity.client.renderer.layer.LunarWolfEyesLayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Wolf;
import org.jetbrains.annotations.NotNull;

public class LunarWolfRenderer extends WolfRenderer {
    private static final ResourceLocation LUNAR_WOLF_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(WolfismMod.MOD_ID, "textures/entity/lunar_wolf/lunar_wolf.png");

    public LunarWolfRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.addLayer(new LunarWolfEyesLayer(this));
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation( Wolf wolf) {
        return LUNAR_WOLF_TEXTURE;
    }

    @Override
    public void render( Wolf entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight ) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
}