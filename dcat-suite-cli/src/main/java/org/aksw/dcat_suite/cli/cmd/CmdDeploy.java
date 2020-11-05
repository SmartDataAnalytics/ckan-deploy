package org.aksw.dcat_suite.cli.cmd;

import picocli.CommandLine.Command;

@Command(name = "deploy", separator = "=", description = "Deploy DCAT datasets", subcommands = {
        CmdDeployCkan.class,
        CmdDeployVirtuoso.class
})
public class CmdDeploy {
}