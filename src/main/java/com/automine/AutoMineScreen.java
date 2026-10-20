package com.automine;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Locale;

/** Layout per scegliere i blocchi: click su un blocco = selezionato / deselezionato. */
public class AutoMineScreen extends Screen {
    private static final int CELL = 20;

    private static final int COLOR_SELECTED = 0xAA2ECC40;
    private static final int COLOR_IDLE = 0x66000000;
    private static final int COLOR_HOVER = 0x55FFFFFF;

    private EditBox search;
    private Button stateButton;
    private Button protectButton;

    private List<Block> all;
    private List<Block> filtered;

    private int scroll = 0;
    private int cols, rows, gridX, gridY;

    public AutoMineScreen() {
        super(Component.translatable("automine.title"));
    }

    @Override
    protected void init() {
        all = AutoMineConfig.allBlocks();

        int panelW = Math.min(width - 20, 460);
        cols = Math.max(1, (panelW - 10) / CELL);
        gridX = (width - cols * CELL) / 2;
        gridY = 50;
        rows = Math.max(1, (height - gridY - 34) / CELL);

        search = new EditBox(font, width / 2 - 100, 22, 200, 18, Component.translatable("automine.search"));
        search.setHint(Component.translatable("automine.search"));
        search.setResponder(text -> {
            scroll = 0;
            applyFilter();
        });
        addRenderableWidget(search);
        setInitialFocus(search);

        int bw = Math.min(110, (panelW - 4 * 4) / 5);
        int by = height - 26;
        int bx = (width - (bw * 5 + 4 * 4)) / 2;

        addRenderableWidget(Button.builder(Component.translatable("automine.clear"), b -> AutoMineConfig.clear())
                .bounds(bx, by, bw, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("automine.ores"), b -> AutoMineConfig.selectOres())
                .bounds(bx + (bw + 4), by, bw, 20).build());

        protectButton = addRenderableWidget(Button.builder(protectLabel(), b -> {
            AutoMineConfig.protectTool = !AutoMineConfig.protectTool;
            b.setMessage(protectLabel());
        }).bounds(bx + 2 * (bw + 4), by, bw, 20).build());

        stateButton = addRenderableWidget(Button.builder(stateLabel(), b -> {
            AutoMineClient.enabled = !AutoMineClient.enabled;
            b.setMessage(stateLabel());
        }).bounds(bx + 3 * (bw + 4), by, bw, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("automine.close"), b -> onClose())
                .bounds(bx + 4 * (bw + 4), by, bw, 20).build());

        applyFilter();
    }

    private Component protectLabel() {
        return Component.translatable("automine.tool_protect",
                Component.translatable(AutoMineConfig.protectTool ? "automine.on" : "automine.off"));
    }

    private Component stateLabel() {
        return Component.translatable("automine.state",
                Component.translatable(AutoMineClient.enabled ? "automine.on" : "automine.off"));
    }

    private void applyFilter() {
        String q = search == null ? "" : search.getValue().toLowerCase(Locale.ROOT).trim();
        if (q.isEmpty()) {
            filtered = all;
        } else {
            filtered = all.stream()
                    .filter(b -> AutoMineConfig.idOf(b).contains(q)
                            || b.getName().getString().toLowerCase(Locale.ROOT).contains(q))
                    .toList();
        }
        clampScroll();
    }

    private int maxScroll() {
        int totalRows = (filtered.size() + cols - 1) / cols;
        return Math.max(0, totalRows - rows);
    }

    private void clampScroll() {
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
    }

    private Block blockAt(double mx, double my) {
        if (mx < gridX || my < gridY) return null;
        int c = (int) ((mx - gridX) / CELL);
        int r = (int) ((my - gridY) / CELL);
        if (c >= cols || r >= rows) return null;
        int idx = (scroll + r) * cols + c;
        return idx >= 0 && idx < filtered.size() ? filtered.get(idx) : null;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        g.drawCenteredString(font, title, width / 2, 8, 0xFFFFFFFF);
        g.drawString(font, Component.translatable("automine.selected", AutoMineConfig.selectedCount()),
                gridX, 30, 0xFFAAAAAA);

        Block hovered = blockAt(mouseX, mouseY);

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int idx = (scroll + r) * cols + c;
                if (idx >= filtered.size()) break;
                Block block = filtered.get(idx);
                int x = gridX + c * CELL;
                int y = gridY + r * CELL;

                g.fill(x, y, x + CELL - 1, y + CELL - 1,
                        AutoMineConfig.isSelected(block) ? COLOR_SELECTED : COLOR_IDLE);
                if (block == hovered) {
                    g.fill(x, y, x + CELL - 1, y + CELL - 1, COLOR_HOVER);
                }
                g.renderItem(AutoMineConfig.stackOf(block), x + 2, y + 2);
            }
        }

        if (hovered != null) {
            g.setTooltipForNextFrame(font, hovered.getName(), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        if (event.button() == 0) {
            Block block = blockAt(event.x(), event.y());
            if (block != null) {
                AutoMineConfig.toggle(block);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll -= (int) Math.signum(scrollY);
        clampScroll();
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        AutoMineConfig.save();
    }
}
