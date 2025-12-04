// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";
import "@openzeppelin/contracts/token/ERC20/extensions/ERC20Burnable.sol";
import "@openzeppelin/contracts/access/Ownable.sol";

contract CyclingToken is ERC20, Ownable, ERC20Burnable {
    // Il costruttore richiede l'indirizzo iniziale dell'owner
    // 'msg.sender' sarà il proprietario (tu che fai il deploy)
    constructor(uint256 initialSupply) 
        ERC20("CyclingToken", "CYCL") 
        Ownable(msg.sender) // Inizializza l'owner
    {
        // Se vuoi partire con zero token, passa 0 come initialSupply
        if (initialSupply > 0) {
            _mint(msg.sender, initialSupply * 10 ** decimals());
        }
    }

    event RideMinted(address indexed user, uint256 amount, string cid);

    // Funzione MINT: Crea nuovi token dal nulla
    // "onlyOwner" significa che SOLO il wallet che ha deployato il contratto può chiamarla
    function mint(address to, uint256 amount, string memory tokenURI) public onlyOwner {
        _mint(to, amount);
        emit RideMinted(to, amount, tokenURI);
    }
}
