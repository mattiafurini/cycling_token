pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";
import "@openzeppelin/contracts/token/ERC20/extensions/ERC20Burnable.sol";
import "@openzeppelin/contracts/access/Ownable.sol";

contract CyclingToken is ERC20, Ownable, ERC20Burnable {
    constructor(
        uint256 initialSupply
    ) ERC20("CyclingToken", "CYCL") Ownable(msg.sender) {
        if (initialSupply > 0) {
            _mint(msg.sender, initialSupply * 10 ** decimals());
        }
    }

    event RideMinted(address indexed user, uint256 amount, string cid);

    function mint(
        address to,
        uint256 amount,
        string memory tokenURI
    ) public onlyOwner {
        _mint(to, amount);
        emit RideMinted(to, amount, tokenURI);
    }
}
