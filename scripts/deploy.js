const hre = require("hardhat");

async function main() {
  const [deployer] = await hre.ethers.getSigners();
  console.log("Deploying with account:", deployer.address);
  
  const initialSupply = 1000000; // 1 milione di token
  const CyclingToken = await hre.ethers.getContractFactory("CyclingToken");
  const token = await CyclingToken.deploy(initialSupply);
  
  await token.deployed();  // ← Cambiato da waitForDeployment()
  
  console.log("CyclingToken deployed to:", token.address);  // ← Cambiato da getAddress()
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
