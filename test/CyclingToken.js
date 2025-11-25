const { expect } = require("chai");
const { ethers } = require("hardhat");

describe("CyclingToken", function () {
    let CyclingToken;
    let cyclingToken;
    let owner;
    let addr1;
    let addr2;

    beforeEach(async function () {
        [owner, addr1, addr2] = await ethers.getSigners();
        CyclingToken = await ethers.getContractFactory("CyclingToken");
        // Deploy with 1 million initial supply
        cyclingToken = await CyclingToken.deploy(1000000);
        await cyclingToken.deployed();
    });

    describe("Deployment", function () {
        it("Should set the right owner", async function () {
            expect(await cyclingToken.balanceOf(owner.address)).to.equal(ethers.utils.parseUnits("1000000", 18));
        });

        it("Should have the correct name and symbol", async function () {
            expect(await cyclingToken.name()).to.equal("CyclingToken");
            expect(await cyclingToken.symbol()).to.equal("CYCL");
        });

        it("Should assign the total supply to the owner", async function () {
            const ownerBalance = await cyclingToken.balanceOf(owner.address);
            expect(await cyclingToken.totalSupply()).to.equal(ownerBalance);
        });
    });

    describe("Transactions", function () {
        it("Should transfer tokens between accounts", async function () {
            // Transfer 50 tokens from owner to addr1
            await cyclingToken.transfer(addr1.address, ethers.utils.parseUnits("50", 18));
            const addr1Balance = await cyclingToken.balanceOf(addr1.address);
            expect(addr1Balance).to.equal(ethers.utils.parseUnits("50", 18));

            // Transfer 50 tokens from addr1 to addr2
            await cyclingToken.connect(addr1).transfer(addr2.address, ethers.utils.parseUnits("50", 18));
            const addr2Balance = await cyclingToken.balanceOf(addr2.address);
            expect(addr2Balance).to.equal(ethers.utils.parseUnits("50", 18));
        });

        it("Should fail if sender doesn't have enough tokens", async function () {
            const initialOwnerBalance = await cyclingToken.balanceOf(owner.address);

            // Try to send 1 token from addr1 (0 balance) to owner
            await expect(
                cyclingToken.connect(addr1).transfer(owner.address, 1)
            ).to.be.reverted; // ERC20 reverts on insufficient balance, specific error depends on implementation but reverted is guaranteed

            // Owner balance shouldn't have changed
            expect(await cyclingToken.balanceOf(owner.address)).to.equal(initialOwnerBalance);
        });
    });
});
