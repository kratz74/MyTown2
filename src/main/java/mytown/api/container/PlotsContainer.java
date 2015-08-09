package mytown.api.container;

import myessentials.entities.Container;
import mytown.datasource.MyTownUniverse;
import mytown.entities.Plot;
import mytown.entities.Resident;
import mytown.entities.TownBlock;
import mytown.handlers.VisualsHandler;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.ArrayList;
import java.util.List;

public class PlotsContainer extends Container<Plot> {

    private int maxPlots;

    public PlotsContainer() {
        this.maxPlots = -1;
    }

    public PlotsContainer(int maxPlots) {
        this.maxPlots = maxPlots;
    }


    public void remove(Plot plot) {
        for (int x = plot.getStartChunkX(); x <= plot.getEndChunkX(); x++) {
            for (int z = plot.getStartChunkZ(); z <= plot.getEndChunkZ(); z++) {
                TownBlock b = MyTownUniverse.instance.getTownBlock(plot.getDim(), x, z);
                if (b != null) {
                    b.plotsContainer.remove(plot);
                }
            }
        }
        items.remove(plot);
    }


    public Plot get(String name) {
        for(Plot plot : items) {
            if(plot.getName().equals(name))
                return plot;
        }
        return null;
    }

    public Plot get(int dim, int x, int y, int z) {
        TownBlock block = MyTownUniverse.instance.getTownBlock(dim, x >> 4, z >> 4);
        if (block != null) {
            return block.plotsContainer.get(dim, x, y, z);
        }
        return null;
    }

    public Plot get(Resident res) {
        return get(res.getPlayer().dimension, (int) Math.floor(res.getPlayer().posX), (int) Math.floor(res.getPlayer().posY), (int) Math.floor(res.getPlayer().posZ));
    }

    public List<Plot> getPlotsOwned(Resident res) {
        List<Plot> list = new ArrayList<Plot>();
        for (Plot plot : items) {
            if (plot.ownersContainer.contains(res))
                list.add(plot);
        }
        return list;
    }

    public int getAmountPlotsOwned(Resident res) {
        int plotsOwned = 0;
        for (Plot plot : items) {
            if (plot.ownersContainer.contains(res))
                plotsOwned++;
        }
        return plotsOwned;
    }

    public int getMaxPlots() {
        return this.maxPlots;
    }

    public void setMaxPlots(int maxPlots) {
        this.maxPlots = maxPlots;
    }

    public boolean canResidentMakePlot(Resident res) {
        return maxPlots == -1 || getAmountPlotsOwned(res) < maxPlots;
    }

    public void show(Resident res) {
        if(res.getPlayer() instanceof EntityPlayerMP) {
            for (Plot plot : items) {
                VisualsHandler.instance.markPlotBorders(plot, (EntityPlayerMP) res.getPlayer());
            }
        }
    }

    public void hide(Resident res) {
        if(res.getPlayer() instanceof EntityPlayerMP) {
            for (Plot plot : items) {
                VisualsHandler.instance.unmarkBlocks((EntityPlayerMP) res.getPlayer(), plot);
            }
        }
    }
}
