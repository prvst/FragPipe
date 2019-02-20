/*
 * Copyright (C) 2018 Dmitry Avtonomov
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package umich.msfragger.params.enums;

/**
 * @author Dmitry Avtonomov
 */
public enum CleavageType {
  ENZYMATIC(2),
  SEMI(1),
  NON_SPECIFIC(0);

  private final int numEnzymeTermini;

  CleavageType(int num_enzyme_termini) {
    this.numEnzymeTermini = num_enzyme_termini;
  }

  public int valueInParamsFile() {
    return numEnzymeTermini;
  }

  public static CleavageType fromValueInParamsFile(String paramsFileRepresentation) {
    for (CleavageType ct : CleavageType.values()) {
      if (Integer.toString(ct.valueInParamsFile()).equals(paramsFileRepresentation)) {
        return ct;
      }
    }
    throw new IllegalArgumentException(
        "Enum CleavageType does not contain a mapping for params file value of '"
            + paramsFileRepresentation + "'");
  }
}
