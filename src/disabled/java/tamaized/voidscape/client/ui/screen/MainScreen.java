package tamaized.voidscape.client.ui.screen;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.util.LazyOptional;
import tamaized.voidscape.Voidscape;
import tamaized.voidscape.client.ClientUtil;
import tamaized.voidscape.client.Shaders;
import tamaized.voidscape.client.StencilBufferUtil;
import tamaized.voidscape.client.ui.RenderTurmoil;
import tamaized.voidscape.network.server.ServerPacketTurmoilProgressTutorial;
import tamaized.voidscape.network.server.ServerPacketTurmoilResetSkills;
import tamaized.voidscape.party.ClientPartyInfo;
import tamaized.voidscape.turmoil.Progression;
import tamaized.voidscape.turmoil.SubCapability;
import tamaized.voidscape.turmoil.Turmoil;
import tamaized.voidscape.turmoil.TurmoilStats;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class MainScreen extends TurmoilScreen {

	private long tick;
	private Button reset;
	private Function<Button, Tooltip> resetTooltip;

	public MainScreen() {
		super(Component.translatable(Voidscape.MODID.concat(".screen.main")));
	}

	@Override
	protected void init() {
		super.init();
		if (minecraft == null || minecraft.player == null)
			return;
		Turmoil data = getData(Voidscape.subCapTurmoilData);
		tick = minecraft.level == null ? 0 : minecraft.level.getGameTime();
		Window window = minecraft.getWindow();
		final int buttonWidth = 180;
		final int buttonHeight = 20;
		final int spacingHeight = (int) (buttonHeight * 1.5F);
		Button teleport = Button.builder(
				Component.translatable("Enter the Void"), // FIXME: localize
				button -> {
					if (minecraft.player != null)
						minecraft.player.getCapability(SubCapability.CAPABILITY).ifPresent(cap -> cap.get(Voidscape.subCapTurmoilData).ifPresent(Turmoil::clientTeleport));
				}
		).bounds(
				(int) (window.getGuiScaledWidth() / 4F - buttonWidth / 2F),
				(int) (window.getGuiScaledHeight() / 4F - buttonHeight / 2F),
				buttonWidth,
				buttonHeight
		).build();
		teleport.active = !Voidscape.checkForVoidDimension(minecraft.level);
		addRenderableWidget(teleport);
		addRenderableWidget(Button.builder(
				Component.translatable("Voidic Powers"), // FIXME: localize
				button -> {
					if (data != null && data.getProgression() == Progression.MidTutorial)
						Voidscape.NETWORK.sendToServer(new ServerPacketTurmoilProgressTutorial());
					minecraft.setScreen(new SkillsScreen());
				}
		).bounds(
				(int) (window.getGuiScaledWidth() / 4F - buttonWidth / 2F),
				(int) (window.getGuiScaledHeight() / 4F - buttonHeight / 2F) + spacingHeight,
				buttonWidth,
				buttonHeight
		).build());
		Button spells = Button.builder(
				Component.translatable("Configure Voidic Spells"), // FIXME: localize
				button -> minecraft.setScreen(new SpellsScreen())
		).bounds(
				(int) (window.getGuiScaledWidth() / 4F - buttonWidth / 2F),
				(int) (window.getGuiScaledHeight() / 4F - buttonHeight / 2F) + spacingHeight * 2,
				buttonWidth,
				buttonHeight
		).build();
		spells.active = data != null && data.hasCoreSkill();
		addRenderableWidget(spells);
		reset = Button.builder(
				Component.translatable("Reset Voidic Skills"), // FIXME: localize
				button -> {
					if (button.active) {
						Voidscape.NETWORK.sendToServer(new ServerPacketTurmoilResetSkills());
						TurmoilStats stats = getData(Voidscape.subCapTurmoilStats);
						if (data != null && stats != null)
							data.resetSkills(stats);
					}
				}
		).bounds(
				(int) (window.getGuiScaledWidth() / 4F - buttonWidth / 2F),
				(int) (window.getGuiScaledHeight() / 4F - buttonHeight / 2F) + spacingHeight * 3,
				buttonWidth,
				buttonHeight
		).build();
		resetTooltip = button -> {
			if (button.active || data == null || !data.hasCoreSkill())
				return null;
			// FIXME: localize
			boolean min = data.getResetCooldown() > 1200;
			String text = data.getResetCooldown() > 0 ? ("%s %s Remaining before you can Reset again") :
					!minecraft.player.inventory.contains(ServerPacketTurmoilResetSkills.VOIDIC_CRYSTAL.get()) ? "Voidic Crystal missing from Inventory" : "";
			if (!text.isEmpty())
				return Tooltip.create(Component.translatable(text, data.getResetCooldown() / (min ? 1200 : 20), min ? "Minutes" : "Seconds"));
			return null;
		};
		reset.active = data != null && data.hasCoreSkill() && data.getResetCooldown() <= 0 && minecraft.player.inventory.contains(ServerPacketTurmoilResetSkills.VOIDIC_CRYSTAL.get());
		addRenderableWidget(reset);
		Button instances = Button.builder(
				Component.translatable("Duties"), // FIXME: localize
				button -> {
					if (ClientPartyInfo.host == null)
						minecraft.setScreen(new DutyScreen());
					else
						minecraft.setScreen(new FormPartyScreen(ClientPartyInfo.duty));
				}
		).bounds(
				(int) (window.getGuiScaledWidth() / 4F - buttonWidth / 2F),
				(int) (window.getGuiScaledHeight() / 4F - buttonHeight / 2F) + spacingHeight * 4,
				buttonWidth,
				buttonHeight
		).build();
		instances.active = data != null && data.getProgression().ordinal() >= Progression.Psychosis.ordinal();
		addRenderableWidget(instances);
		addRenderableWidget(Button.builder(
				Component.translatable("Close"), // FIXME: localize
				button -> onClose()
		).bounds(
				(int) (window.getGuiScaledWidth() / 2F - buttonWidth / 2F),
				window.getGuiScaledHeight() - buttonHeight - 5,
				buttonWidth,
				buttonHeight
		).build());
	}

	private void updateTooltips() {
		reset.setTooltip(resetTooltip.apply(reset));
	}

	@Override
	public void tick() {
		super.tick();
		Turmoil data = getData(Voidscape.subCapTurmoilData);
		reset.active = minecraft != null && minecraft.player != null && data != null && data.hasCoreSkill() && data.getResetCooldown() <= 0 && minecraft.player.inventory.contains(ServerPacketTurmoilResetSkills.VOIDIC_CRYSTAL.get());
	}

	@Override
	public void render(PoseStack p_230430_1_, int p_230430_2_, int p_230430_3_, float p_230430_4_) {
		if (minecraft == null || minecraft.level == null) {
			onClose();
			return;
		}
		updateTooltips();
		RenderSystem.enableBlend();
		{

			Window window = Minecraft.getInstance().getWindow();

			float x = 0F;
			float y = 0F;
			float w = window.getGuiScaledWidth();
			float h = window.getGuiScaledHeight();
			float z = 0F;

			ClientUtil.bindTexture(RenderTurmoil.TEXTURE_MASK);
			RenderTurmoil.Color24.INSTANCE.set(1F, 1F, 1F, 1F).apply(true, x, y, z, w, h);
			final int stencilIndex = 12;
			float perc = Math.min(1F, (minecraft.level.getGameTime() - tick) / (20 * 3F));
			StencilBufferUtil.setup(stencilIndex, () -> Shaders.OPTIMAL_ALPHA_LESSTHAN_POS_TEX_COLOR.invokeThenEndTesselator(perc));
			StencilBufferUtil.renderAndFlush(stencilIndex, () -> super.render(p_230430_1_, p_230430_2_, p_230430_3_, p_230430_4_));
		}
		RenderSystem.disableBlend();
		if (minecraft == null || minecraft.player == null) {
			onClose();
			return;
		}
		LazyOptional<SubCapability.ISubCap> o = minecraft.player.getCapability(SubCapability.CAPABILITY);
		if (!o.isPresent()) {
			onClose();
			return;
		}
		o.ifPresent(cap -> {
			Optional<Turmoil> t = cap.get(Voidscape.subCapTurmoilData);
			if (t.isEmpty()) {
				onClose();
				return;
			}
			t.ifPresent(data -> {
				if (data.getState() != Turmoil.State.OPEN)
					onClose();
			});
		});
	}
}