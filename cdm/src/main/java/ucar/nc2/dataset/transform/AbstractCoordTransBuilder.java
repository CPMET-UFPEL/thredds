/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.nc2.dataset.transform;

import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Dimension;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.*;
import ucar.unidata.util.Parameter;
import ucar.units.UnitFormat;
import ucar.units.UnitFormatManager;
import ucar.units.Unit;

import java.util.StringTokenizer;
import java.util.List;

/**
 * Abstract superclass for implementations of CoordTransBuilderIF.
 *
 * @author caron
 */
public abstract class AbstractCoordTransBuilder implements ucar.nc2.dataset.CoordTransBuilderIF {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AbstractCoordTransBuilder.class);
  protected StringBuffer errBuffer = null;

  public void setErrorBuffer(StringBuffer errBuffer) {
    this.errBuffer = errBuffer;
  }

  public ucar.unidata.geoloc.vertical.VerticalTransform makeMathTransform(NetcdfDataset ds, Dimension timeDim, VerticalCT vCT) {
    throw new UnsupportedOperationException();
  }

  /**
   * Read a variable attribute as a double.
   *
   * @param v       the variable
   * @param attname name of variable
   * @return attribute value as a double, else NaN if not found
   */
  protected double readAttributeDouble(Variable v, String attname) {
    Attribute att = v.findAttributeIgnoreCase(attname);
    if (att == null) return Double.NaN;
    if (att.isString())
      return Double.parseDouble(att.getStringValue());
    else
      return att.getNumericValue().doubleValue();
  }

  /**
   * Read an attribute as double[2]. If only one value, make second same as first.
   *
   * @param att the attribute. May be numeric or String.
   * @return attribute value as a double[2]
   */
  protected double[] readAttributeDouble2(Attribute att) {
    if (att == null) return null;

    double[] val = new double[2];
    if (att.isString()) {
      StringTokenizer stoke = new StringTokenizer(att.getStringValue());
      val[0] = Double.parseDouble(stoke.nextToken());
      val[1] = stoke.hasMoreTokens() ? Double.parseDouble(stoke.nextToken()) : val[0];
    } else {
      val[0] = att.getNumericValue().doubleValue();
      val[1] = (att.getLength() > 1) ? att.getNumericValue(1).doubleValue() : val[0];
    }
    return val;
  }

  /**
   * Add a Parameter to a CoordinateTransform.
   * Make sure that variable exist. If readData is true, read the data and use it as the value of the
   * parameter, otherwise use the variable name as the value of the parameter.
   *
   * @param rs        the CoordinateTransform
   * @param paramName the parameter name
   * @param ds        dataset
   * @param varName   variable name
   * @return true if success, false is failed
   */
  protected boolean addParameter(CoordinateTransform rs, String paramName, NetcdfFile ds, String varName) {
    if (null == (ds.findVariable(varName))) {
      if (null != errBuffer)
        errBuffer.append("CoordTransBuilder ").append(getTransformName()).append(": no Variable named ").append(varName);
      return false;
    }

    rs.addParameter(new Parameter(paramName, varName));
    return true;
  }

  protected String getFormula(NetcdfDataset ds, Variable ctv) {
    String formula = ds.findAttValueIgnoreCase(ctv, "formula_terms", null);
    if (null == formula) {
      if (null != errBuffer)
        errBuffer.append("CoordTransBuilder ").append(getTransformName()).append(": needs attribute 'formula_terms' on Variable ").append(ctv.getName()).append("\n");
      return null;
    }
    return formula;
  }


  //////////////////////////////////////////
  //private UnitFormat format;
  static public double getFalseEastingScaleFactor(NetcdfDataset ds, Variable ctv) {
    String units = ds.findAttValueIgnoreCase(ctv, "units", null);
    if (units == null) {
      List<CoordinateAxis> axes = ds.getCoordinateAxes();
      for (CoordinateAxis axis : axes) {
        if (axis.getAxisType() == AxisType.GeoX) { // kludge - what if there's multiple ones?
          Variable v = axis.getOriginalVariable();
          units = v.getUnitsString();
          break;
        }
      }
    }
    if (units != null) {
      try {
        UnitFormat format = UnitFormatManager.instance();
        Unit uuInput = format.parse(units);
        Unit uuOutput = format.parse("km");
        return uuInput.convertTo(1.0, uuOutput);
      } catch (Exception e) {
        log.error(units + " not convertible to km");
      }
    }
    return 1.0;
  }

}
