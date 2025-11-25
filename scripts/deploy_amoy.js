const hre = require("hardhat");

async function main() {
    const [deployer] = await hre.ethers.getSigners();
    console.log("Deploying to Polygon Amoy with account:", deployer.address);

    const initialSupply = 1000000; // 1 million tokens
    const CyclingToken = await hre.ethers.getContractFactory("CyclingToken");
    const token = await CyclingToken.deploy(initialSupply);

    await token.deployed();

    console.log("CyclingToken deployed to:", token.address);
    console.log("Verify on Amoy Explorer: https://amoy.polygonscan.com/address/" + token.address);
}

main().catch((error) => {
    console.error(error);
    process.exitCode = 1;
});
