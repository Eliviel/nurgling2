package nurgling.actions;

import haven.Coord2d;
import haven.Gob;
import nurgling.NGameUI;
import nurgling.NGob;
import nurgling.NUtils;

import static nurgling.tools.Finder.findLiftedbyPlayer;

public class PlaceObject implements Action {
    @Override
    public Results run ( NGameUI gui )
            throws InterruptedException {
        if(gob == null)
            gob = findLiftedbyPlayer();
        if ( gob != null ) {
            if(dynamicpf) {
                DynamicPf dpf = new DynamicPf(NGob.getDummy(pos, a, gob.ngob.hitBox), true);
                dpf.isHardMode = true;
                dpf.run(gui);
            }
            else {
                PathFinder pf = new PathFinder(NGob.getDummy(pos, a, gob.ngob.hitBox), true);
                pf.isHardMode = true;
                pf.run(gui);
            }
            NUtils.place(gob,pos,a);
            return Results.SUCCESS();
        }
        return Results.ERROR("No gob for place");
    }

    public PlaceObject(
            Gob gob,
            Coord2d pos,
            double a,
            boolean dynamicpf

    ) {
        this.gob = gob;
        this.pos = pos;
        this.a = a;
        this.dynamicpf = dynamicpf;
    }

    public PlaceObject(
            Gob gob,
            Coord2d pos,
            double a

    ) {
        this(gob,pos, a, false);
    }

    Gob gob = null;
    Coord2d pos = null;
    double a = 0;
    boolean dynamicpf = false;
}