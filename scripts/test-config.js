const hre = require("hardhat");

async function main() {
  console.log("Networks disponibili:");
  console.log(hre.config.networks);
}

main();
