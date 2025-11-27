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

        // Deploy con 1 milione di supply iniziale (come prima)
        // NOTA: Se usi ethers v6, .deployed() è deprecato, usa .waitForDeployment()
        // Mantengo .deployed() per compatibilità con il tuo codice precedente
        cyclingToken = await CyclingToken.deploy(1000000);
        await cyclingToken.deployed();
    });

    describe("Deployment", function () {
        it("Should set the right owner", async function () {
            // Verifica che l'owner del contratto sia settato correttamente (funzione di Ownable)
            expect(await cyclingToken.owner()).to.equal(owner.address);
        });

        it("Should assign the initial supply to the owner", async function () {
            expect(await cyclingToken.balanceOf(owner.address)).to.equal(ethers.utils.parseUnits("1000000", 18));
        });

        it("Should have the correct name and symbol", async function () {
            expect(await cyclingToken.name()).to.equal("CyclingToken");
            expect(await cyclingToken.symbol()).to.equal("CYCL");
        });
    });

    describe("Minting (New Feature)", function () {
        it("Should allow owner to mint tokens to any address", async function () {
            // Owner minta 100 token per addr1
            await cyclingToken.mint(addr1.address, ethers.utils.parseUnits("100", 18));

            // Verifica saldo addr1
            expect(await cyclingToken.balanceOf(addr1.address)).to.equal(ethers.utils.parseUnits("100", 18));

            // Verifica che la Total Supply sia aumentata (1M iniziali + 100 nuovi)
            expect(await cyclingToken.totalSupply()).to.equal(ethers.utils.parseUnits("1000100", 18));
        });

        it("Should FAIL if non-owner tries to mint", async function () {
            // addr1 prova a mintare per se stesso -> DEVE FALLIRE
            // Ownable lancia l'errore "Ownable: caller is not the owner"
            await expect(
                cyclingToken.connect(addr1).mint(addr1.address, ethers.utils.parseUnits("100", 18))
            ).to.be.revertedWith("Ownable: caller is not the owner").to.be.reverted;
        });
    });

    describe("Standard Transactions", function () {
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
            ).to.be.reverted;

            // Owner balance shouldn't have changed
            expect(await cyclingToken.balanceOf(owner.address)).to.equal(initialOwnerBalance);
        });
    });
});
