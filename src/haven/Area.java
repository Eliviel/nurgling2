/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import nurgling.NUtils;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

import java.util.*;

public class Area implements Iterable<Coord>, java.io.Serializable {
    public Coord ul, br;

    public Area(Coord ul, Coord br) {
	this.ul = ul;
	this.br = br;
    }

    public Area(Coord ul, Coord br, boolean forced) {
        this.ul = new Coord(Math.min(ul.x,br.x),Math.min(ul.y,br.y));
        this.br = new Coord(Math.max(ul.x,br.x),Math.max(ul.y,br.y));
    }

    public int hashCode() {
	return((ul.hashCode() * 31) + br.hashCode());
    }

    public boolean equals(Object o) {
	if(!(o instanceof Area))
	    return(false);
	Area a = (Area)o;
	return(a.ul.equals(ul) && a.br.equals(br));
    }

    public static Area corn(Coord ul, Coord br) {
	return(new Area(ul, br));
    }

    public static Area corni(Coord ul, Coord bri) {
	return(new Area(ul, bri.add(1, 1)));
    }

    public static Area sized(Coord ul, Coord sz) {
	return(new Area(ul, ul.add(sz)));
    }

    public static Area sized(Coord sz) {
	return(new Area(Coord.z, sz));
    }

    public static Area sizedi(Coord szi) {
	return(new Area(Coord.z, szi.add(1, 1)));
    }

    public Coord sz() {
	return(br.sub(ul));
    }

    public boolean positive() {
	return((br.x > ul.x) && (br.y > ul.y));
    }

    public boolean contains(Coord c) {
	return((c.x >= ul.x) && (c.y >= ul.y) && (c.x < br.x) && (c.y < br.y));
    }

    public boolean isects(Area o) {
	return((br.x > o.ul.x) && (br.y > o.ul.y) && (o.br.x > ul.x) && (o.br.y > ul.y));
    }

    public boolean contains(Area o) {
	return((o.ul.x >= ul.x) && (o.ul.y >= ul.y) && (o.br.x <= br.x) && (o.br.y <= br.y));
    }

    public Area overlap(Area o) {
	if(!isects(o))
	    return(null);
	return(corn(Coord.of(Math.max(ul.x, o.ul.x), Math.max(ul.y, o.ul.y)),
		    Coord.of(Math.min(br.x, o.br.x), Math.min(br.y, o.br.y))));
    }

    public Coord closest(Coord p) {
	if(contains(p))
	    return(p);
	return(Coord.of(Utils.clip(p.x, ul.x, br.x),
			Utils.clip(p.y, ul.y, br.y)));
    }

    public int area() {
	return((br.x - ul.x) * (br.y - ul.y));
    }

    public Area xl(Coord off) {
	return(corn(ul.add(off), br.add(off)));
    }

    public Area margin(Coord m) {
	return(corn(ul.sub(m), br.add(m)));
    }

    public Area margin(int m) {
	return(margin(Coord.of(m, m)));
    }

    public Area mul(Coord d) {
	return(corn(ul.mul(d), br.mul(d)));
    }

    public Area div(Coord d) {
	return(corn(ul.div(d), br.sub(1, 1).div(d).add(1, 1)));
    }

    public int ridx(Coord c) {
	if(!contains(c))
	    return(-1);
	return((c.x - ul.x) + ((c.y - ul.y) * (br.x - ul.x)));
    }

    public Iterator<Coord> iterator() {
	return(new Iterator<Coord>() {
		int x = ul.x, y = ul.y;

		public boolean hasNext() {
		    return((y < br.y) && (x < br.x));
		}

		public Coord next() {
		    Coord ret = Coord.of(x, y);
		    if(++x >= br.x) {
			x = ul.x;
			y++;
		    }
		    return(ret);
		}
	    });
    }

    public int ri(Coord c) {
	return((c.x - ul.x) + ((c.y - ul.y) * (br.x - ul.x)));
    }

    public int rsz() {
	return((br.x - ul.x) * (br.y - ul.y));
    }

    public String toString() {
	return(String.format("((%d, %d) - (%d, %d))", ul.x, ul.y, br.x, br.y));
    }


    public static class Tile
    {
        Coord pos;
        public String name;
        public boolean isFree = false;
        public List<Gob> gobs = new ArrayList<>();

        public Tile(Coord pos, String name, boolean isFree) {
            this.pos = pos;
            this.name = name;
            this.isFree = isFree;
        }
    }

    public static Tile[][]
    getTiles(Area a, NAlias names) {
        Coord pos = new Coord(a.ul);
        Tile[][] res = new Tile[a.br.x-a.ul.x+1][a.br.y-a.ul.y+1];
        int i = 0;
        int j = 0;
        while(pos.x<=a.br.x) {
            while (pos.y <= a.br.y) {
                Resource res_beg = NUtils.getGameUI().ui.sess.glob.map.tilesetr ( NUtils.getGameUI().ui.sess.glob.map.gettile ( pos ) );
                res[i][j] = new Tile(pos, res_beg.name, (names==null)?Finder.findGob(pos)==null:Finder.findGob(pos,names)==null);
                res[i][j].gobs = Finder.findGobs(pos);
                pos.y += 1;
                j++;
            }
            j = 0;
            pos.y = a.ul.y;
            pos.x ++;
            i++;
        }
        return res;
    }

    public String space()
    {
        return "Area(" + (br.x-ul.x) + " , " +(br.y-ul.y) +")";
    }
}
