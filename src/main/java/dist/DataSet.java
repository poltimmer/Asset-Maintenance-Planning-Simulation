/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dist;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 *
 * @author MBoon
 */
public class DataSet {

  private String[] varNames; // variable names
  private double[][] values; // values
  
  public DataSet(String[] variableNames, double[][] values) {
      this.varNames = new String[variableNames.length];
      this.values = new double[values.length][values[0].length];
      for (int i = 0; i < variableNames.length; i++) {
          this.varNames[i] = variableNames[i];
          for (int j = 0; j < values.length; j++) {
              this.values[j][i] = values[j][i];
          }
      }
  }

  public double[][] getValues() {
      return values;
  }
  
  public String[] getVariableNames() {
      return varNames;
  }
  
  public void export(File fileName) {
      try {
          PrintWriter pw = new PrintWriter(new FileWriter(fileName));
          for (int i = 0; i < varNames.length; i++) {
              pw.print(varNames[i] + "\t");
          }
          pw.println();
          for (int j = 0; j < values.length; j++) {
              for (int i = 0; i < values[j].length; i++) {
                  pw.print(values[j][i]);
                  if (i < values[j].length - 1) { // not yet end of line -> next column
                      pw.print("\t"); // next column
                  } else {
                      pw.println();  // next line
                  }
              }
          }
          pw.flush();
          pw.close();
      } catch (IOException e) {
          System.err.println("Error exporting file: " + e.getMessage());
      }
  }
  
  public static void main(String[] arg) {
      String[] vars = {"Stud.Id.", "Grade"};
      double[][] values = {
          {345346, 8.5},
          {399596, 9.1},
          {435231, 6.5},
          {234565, 8.2}
      };
      DataSet data = new DataSet(vars, values);
      data.export(new File("D:/temp/grades.txt"));
  }
}
