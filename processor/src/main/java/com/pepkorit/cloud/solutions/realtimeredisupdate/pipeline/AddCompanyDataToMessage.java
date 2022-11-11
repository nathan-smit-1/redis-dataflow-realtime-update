package com.pepkorit.cloud.solutions.realtimeredisupdate.pipeline;

import com.google.common.flogger.FluentLogger;
import java.util.Map;


import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.PCollectionView;

public class AddCompanyDataToMessage extends DoFn<BranchCompanySkuTransactionValue, BranchCompanySkuTransactionValue> {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  private final PCollectionView<Map<String, String>> branches;

    public AddCompanyDataToMessage(PCollectionView<Map<String, String>> branches) {
        this.branches = branches;
    }

    private Map<String, String> branchesTable;
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  @ProcessElement
  public void ProcessElement(ProcessContext c) {
      branchesTable = c.sideInput(branches);
      String branch = c.element().getBranch();
      
      logger.atInfo().log("Company value should be " + branchesTable.get(branch));

        // check if the token is a key in the "branches" side input
        if (branchesTable.containsKey(branch)) {
          c.output(
            new BranchCompanySkuTransactionValue(
              branch,
              branchesTable.get(branch),
              c.element().getSku(),
              c.element().getTransactionId(),
              c.element().getTable(),
              c.element().getValue()
            )
          );
        }
      

  }
}
