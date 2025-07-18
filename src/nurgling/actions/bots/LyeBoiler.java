package nurgling.actions.bots;

import haven.Coord2d;
import haven.Gob;
import haven.Pair;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tasks.WaitForFirstBurnout;
import nurgling.tools.Container;
import nurgling.tools.Context;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;


import java.util.ArrayList;

public class LyeBoiler implements Action {
    String cap = "Cauldron";
    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        NArea.Specialisation rboiler = new NArea.Specialisation(Specialisation.SpecName.boiler.toString());
        NArea.Specialisation rwater = new NArea.Specialisation(Specialisation.SpecName.water.toString());

        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(rboiler);
        req.add(rwater);
        ArrayList<NArea.Specialisation> opt = new ArrayList<>();
        if (new Validator(req, opt).run(gui).IsSuccess()) {


            NArea cauldrons = NContext.findSpec(Specialisation.SpecName.boiler.toString());

            ArrayList<Container> containers = new ArrayList<>();
            for (Gob cm : Finder.findGobs(cauldrons, new NAlias("gfx/terobjs/cauldron"))) {
                Container cand = new Container(cm, cap);

                cand.initattr(Container.Space.class);
                cand.initattr(Container.FuelLvl.class);
                cand.initattr(Container.WaterLvl.class);
                cand.getattr(Container.WaterLvl.class).setMaxlvl(30);

                cand.getattr(Container.FuelLvl.class).setAbsMaxlvl(50);
                cand.getattr(Container.FuelLvl.class).setMaxlvl(40);
                cand.getattr(Container.FuelLvl.class).setFuelmod(5);//branch gives 5 fuel, not 1
                cand.getattr(Container.FuelLvl.class).setFueltype("branch");

                containers.add(cand);
            }

            ArrayList<Gob> lighted = new ArrayList<>();
            for (Container cont : containers) {
                lighted.add(Finder.findGob(cont.gobid));
            }

            Results res = null;
            Context icontext = new Context();
            Pair<Coord2d, Coord2d> area = NContext.findIn(new NAlias("Ashes")).getRCArea();
            if (area == null) {
                return Results.ERROR("Ashes not found");
            }
            ArrayList<Gob> barrels = Finder.findGobs(area, new NAlias("barrel"));
            for (Gob gob : barrels) {
                if (NUtils.barrelHasContent(gob) && NUtils.isOverlay(gob, new NAlias("ash"))) {
                    icontext.addInput("Ashes", new Context.InputBarrel(gob));
                }
            }


            while (res == null || res.IsSuccess()) {
                NUtils.getUI().core.addTask(new WaitForFirstBurnout(lighted, 12));
                synchronized (NUtils.getGameUI()) {
                    new FreeContainers(containers, new NAlias("Lye")).run(gui);
                    res = new FillContainersFromAreas(containers, new NAlias("Ashes"), icontext).run(gui);

                    ArrayList<Container> forFuel = new ArrayList<>();
                    for (Container container : containers) {
                        Container.Space space = container.getattr(Container.Space.class);
                        if (!space.isEmpty() && (Finder.findGob(container.gobid).ngob.getModelAttribute() & 1) != 1)
                            forFuel.add(container);
                    }

                    new FillFluid(containers, NContext.findSpec(Specialisation.SpecName.water.toString()).getRCArea(), new NAlias("water"), 4).run(gui);
                    if (!new FuelToContainers(forFuel).run(gui).IsSuccess())
                        return Results.ERROR("NO FUEL");

                    ArrayList<Long> flighted = new ArrayList<>();
                    for (Container cont : containers) {
                        if (((Finder.findGob(cont.gobid).ngob.getModelAttribute() & 1) == 1) && (Finder.findGob(cont.gobid).ngob.getModelAttribute() & 2) != 2)
                            flighted.add(cont.gobid);
                    }
                    new LightGob(flighted, 2).run(gui);
                }
            }
        }
        return Results.SUCCESS();
    }
}
