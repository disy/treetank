/**
 * Copyright (c) 2011, University of Konstanz, Distributed Systems Group
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.treetank.xmlprague;

import java.io.File;
import java.util.Properties;

import com.treetank.TestHelper;
import com.treetank.TestHelper.PATHS;
import com.treetank.access.Database;
import com.treetank.access.WriteTransaction;
import com.treetank.api.IDatabase;
import com.treetank.api.ISession;
import com.treetank.api.IWriteTransaction;
import com.treetank.exception.AbsTTException;
import com.treetank.service.xml.shredder.EShredderInsert;
import com.treetank.service.xml.shredder.XMLShredder;

import org.perfidix.AbstractConfig;
import org.perfidix.Benchmark;
import org.perfidix.annotation.AfterEachRun;
import org.perfidix.annotation.Bench;
import org.perfidix.element.KindOfArrangement;
import org.perfidix.meter.AbstractMeter;
import org.perfidix.meter.Time;
import org.perfidix.meter.TimeMeter;
import org.perfidix.ouput.AbstractOutput;
import org.perfidix.ouput.CSVOutput;
import org.perfidix.ouput.TabularSummaryOutput;
import org.perfidix.result.BenchmarkResult;

public class ShredBench {

    private XMLShredder shredderNone;

    private static final int RUNS = 100;

    public static File XMLFile = new File("src" + File.separator + "main" + File.separator + "resources"
        + File.separator + "small.xml");
    public static final File TNKFolder = new File("tnk");

    private int counter = 0;

    public void beforeFirst() {
        final Properties props = new Properties();
        props.put("", "");
    }

    public void beforeShred() {
        try {
            System.out.println("Starting Shredding " + counter);
            final IDatabase database = Database.openDatabase(new File(TNKFolder, XMLFile.getName() + ".tnk"));
            final ISession session = database.getSession();
            final IWriteTransaction wtx = session.beginWriteTransaction();
            if (wtx.moveToFirstChild()) {
                wtx.remove();
            }
            shredderNone =
                new XMLShredder(wtx, XMLShredder.createReader(XMLFile), EShredderInsert.ADDASFIRSTCHILD);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Bench(beforeEachRun = "beforeShred", afterEachRun = "tearDown", runs = RUNS)
    public void benchInsert() {
        try {
            shredderNone.call();
        } catch (AbsTTException e) {
            e.printStackTrace();
        }
    }

    public void tearDown() {
        try {
            System.out.println("Finished Shredding Version " + counter);
            counter++;
            Database.forceCloseDatabase(new File(TNKFolder, XMLFile.getName() + ".tnk"));
        } catch (AbsTTException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void main(final String[] args) {

        if (args.length != 2) {
            System.out
                .println("Please use java -jar JAR \"folder with xmls to parse\" \"folder to write csv\"");
            System.exit(-1);
        }

        // Argument is a folder with only XML in there. For each XML one benchmark should be executed.
        final File filetoshred = new File(args[0]);
        final File[] files = filetoshred.listFiles();
        final File filetoexport = new File(args[1]);
        for (final File currentFile : files) {
            XMLFile = currentFile;
            System.out.println("Starting benchmark for " + XMLFile.getName());
            final int index = currentFile.getName().lastIndexOf(".");
            final File folder = new File(filetoexport, currentFile.getName().substring(0, index));
            folder.mkdirs();
            final FilesizeMeter meter =
                new FilesizeMeter(new File(new File(new File(TNKFolder, XMLFile.getName() + ".tnk"), "tt"),
                    "tt.tnk"));

            final Benchmark bench = new Benchmark(new AbstractConfig(RUNS, new AbstractMeter[] {
                meter, new TimeMeter(Time.MilliSeconds)
            }, new AbstractOutput[0], KindOfArrangement.SequentialMethodArrangement, 1.0d) {
            });

            bench.add(ShredBench.class);
            final BenchmarkResult res = bench.run();
            new TabularSummaryOutput(System.out).visitBenchmark(res);
            new CSVOutput(folder).visitBenchmark(res);
            System.out.println("Finished benchmark for " + XMLFile.getName());
        }

    }
}
