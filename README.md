# BingoScape

[![CI](https://github.com/bingoscape/bingoscape-companion/actions/workflows/ci.yml/badge.svg)](https://github.com/bingoscape/bingoscape-companion/actions/workflows/ci.yml)

A RuneLite plugin for participating in Bingo events with your clan or friends in Old School RuneScape.

## Features

- Connect to BingoScape events using your API key
- View and participate in active bingo events
- Display bingo boards with customizable tiles
- Submit screenshots as evidence for completed tasks
- Track submission status (pending, accepted, declined, requires interaction)
- View team information and member roles
- Monitor your progress through an overlay
- Play progressive (tiered) and battleship boards, with their own overlay, side panel summary and notifications

## Getting Started

1. Install the BingoScape plugin from the RuneLite Plugin Hub
2. Enter your BingoScape API key in the plugin configuration
3. Access the BingoScape panel from the side navigation menu
4. Select an event and bingo board to start participating

## How to Use

### Viewing Events and Bingo Boards

1. Click on the BingoScape icon in the side panel
2. Select an event from the dropdown menu
3. Choose a bingo board from the available options
4. Click "Show Bingo Board" to view the full bingo grid

### Completing Tiles

1. Open the bingo board
2. Click on a tile to view its details
3. Complete the task as specified
4. Click "Take & Review Screenshot" to capture evidence
5. Review your screenshot and click "Submit"
6. Wait for an admin to review your submission

### Tracking Progress

- The color of each tile indicates its status:
    - Default: Not submitted
    - Blue: Pending review
    - Green: Accepted/Completed
    - Yellow: Requires interaction
    - Red: Declined

- Hover over tiles to see detailed information
- An overlay can display your current bingo's codephrase for easy reference

### Board Types

The side panel shows a badge with the type of the selected board. Counts in the panel leave out blind tiles and
the tiles of locked tiers, so they only cover what you can currently see.

**Standard** boards are a plain grid of tiles.

**Progression boards** are split into tiers, shown as bands in the overlay:

- Each band header reads "Tier N  earned/required XP". The XP earned in a tier unlocks the next one.
- A locked tier shows "Locked: N XP needed" and a lock instead of its tiles' content. Locked tiles cannot be
  submitted, and the plugin does not auto-submit them.
- The side panel lists every tier with an XP bar, and what a locked tier still needs.
- When your team unlocks a tier, a popup announces it.
- The "Locked Tier Tiles" option dims locked tiles or hides them so only the tier header remains.

**Battleship boards** hide their tiles ("?" cells, "Tile N") until the event starts, and nothing can be submitted
before then. Afterwards your team's shots are drawn on the tiles:

- Hit: a red X
- Miss: a hollow dot on a dimmed tile
- Sunk: a ship in a ring

The board shows "Ships remaining: X/Y" for the opponents' fleet, and the side panel counts your hits, misses and
sunk ships. Opponent ship positions are never sent to the plugin. When a shot lands, a popup says which tile and
what the result was. If a tile is both sunk and hit, sunk wins; a hit wins over a miss.

Popups for tier unlocks and shots only appear when a board refresh shows something new. Nothing is announced
when a board is first loaded, when you switch boards or teams, or for results that were already there.

### Board Refresh

The plugin reloads the board when a team tile completion comes in, when you press Reload, and after a
submission. A reload sends the last `ETag` back, so an unchanged board costs the server a `304` and the plugin
keeps what it has. The board endpoint allows 60 requests a minute per user.

## Configuration Options

- **API Key**: Your BingoScape authentication key
- **API Base URL**: The URL for the BingoScape service
- **Show Notifications**: Enable/disable notifications for bingo updates
- **Show Bingo Codephrase Overlay**: Display the current bingo's codephrase on screen

Bingo Board:

- **Locked Tier Tiles** (`progressionShowLockedTiles`): Show tiles of locked progression tiers dimmed with a lock (default), or hide them so only the tier header remains
- **Battleship Marks** (`battleshipShowMarkers`): Draw your team's hit, miss and sunk marks on battleship boards (default on). The "Ships remaining" line stays either way

Notifications (the drop popup must be enabled for the popups below):

- **Show Tier Unlocks** (`showTierUnlockNotifications`): Popup when your team unlocks a tier on a progression board (default on)
- **Show Battleship Shots** (`showBattleshipNotifications`): Popup for each hit, miss and sunk ship of your team (default on)
- **Battleship Sound** (`battleshipSound`): Play a sound when a shot lands (default off)

## For Event Organizers

BingoScape allows you to:
- Create custom bingo boards with various challenges
- Assign weights (XP values) to different tasks
- Set up team competitions
- Review and approve submissions
- Track event progress

Visit [next.bingoscape.org](https://next.bingoscape.org) to set up your own bingo events.

## Development

Build and run the tests with JDK 11 or newer:

```
./gradlew build
```

Pull requests and pushes to `master` are checked by the [CI workflow](.github/workflows/ci.yml), which runs the same command.

## Acknowledgments

Parts of this plugin were inspired by other RuneLite plugins:

- [OSRS TCG](https://github.com/Azderi/osrs-tcg) in-game overlay
- [Collection Log Popup Enhanced](https://github.com/TimHeessels/collection-log-popup-enhanced) drop popup

## Support

For issues or feature requests, please open an issue on the GitHub repository.
