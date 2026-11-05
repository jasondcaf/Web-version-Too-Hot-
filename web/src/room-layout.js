/**
 * Normal-room button geometry transcribed from the Swing buildButtons methods.
 * Coordinates share the 800 × 455 logical space used to paint the room art.
 * This module changes presentation only; actions.js remains the rule authority.
 */
export const ROOM_WIDTH = 800;
export const ROOM_HEIGHT = 455;

const rectangles = {
  LIVING_ROOM: {
    shoo: [200, 300, 400, 38], tv: [18, 300, 192, 36],
    vacuum: [218, 300, 178, 36], 'clean-mess': [590, 300, 192, 36],
    window: [18, 342, 170, 36], 'tidy-living': [200, 342, 165, 36],
    'change-LivingRoom': [380, 342, 165, 36], 'fix-ac': [558, 342, 224, 36],
    'ac-0': [20, 384, 180, 30], 'ac-1': [208, 384, 180, 30],
    'ac-2': [396, 384, 180, 30], 'ac-3': [584, 384, 180, 30],
  },
  KITCHEN: {
    'drink-kitchen': [18, 300, 185, 34], dishes: [18, 340, 185, 34],
    sweep: [18, 380, 185, 32], 'feed-kitchen': [215, 300, 180, 34],
    'window-kitchen': [215, 340, 180, 34], 'trash-bag': [215, 380, 180, 32],
    'change-Kitchen': [215, 380, 180, 32],
    'pickup-food': [603, 283, 185, 30], freezer: [603, 283, 185, 30],
    'ice-cream': [603, 283, 185, 30], microwave: [603, 317, 185, 34],
    'eat-cold': [603, 356, 185, 30], 'tidy-kitchen': [603, 390, 185, 28],
  },
  BEDROOM: {
    'feed-bedroom': [22, 300, 195, 36], 'window-bedroom': [22, 342, 195, 34],
    charge: [22, 382, 195, 30], 'rest-bedroom': [305, 300, 190, 36],
    'change-Bedroom': [305, 342, 190, 34], 'dry-off': [305, 382, 190, 30],
    'change-clothes': [305, 382, 190, 30], nap: [305, 382, 190, 30],
    homework: [583, 300, 195, 36], stool: [583, 342, 195, 34],
    'tidy-bedroom': [583, 382, 195, 30],
  },
  GARAGE: {
    'change-Garage': [22, 300, 190, 36], wrench: [22, 342, 190, 36],
    roaches: [230, 300, 280, 38], rats: [230, 300, 280, 38],
    'roaches-cleared': [230, 300, 280, 38], circuit: [560, 300, 220, 38],
  },
  MOMS_ROOM: {
    spray: [22, 300, 220, 36], flashlight: [22, 342, 220, 36],
    'rest-mom': [22, 342, 220, 36], closet: [560, 300, 220, 36],
    'change-MomsRoom': [560, 342, 220, 36], 'leave-closet': [300, 300, 200, 40],
  },
  BATHROOM: {
    shower: [22, 300, 220, 36], 'drink-bathroom': [22, 342, 220, 36],
    'medicine-cabinet': [558, 300, 220, 36], 'change-Bathroom': [558, 342, 220, 36],
    mop: [22, 384, 220, 30],
  },
  BASEMENT: {
    washer: [210, 290, 380, 38], dryer: [260, 290, 280, 38],
    fold: [220, 290, 360, 38], 'new-load': [260, 340, 280, 38],
    'pickup-detergent': [50, 390, 240, 36],
  },
  OUTSIDE: {
    trash: [22, 300, 195, 36], mail: [22, 342, 195, 36],
    'find-mittens': [22, 384, 195, 30], 'start-mowing': [235, 300, 195, 36],
    mow: [235, 300, 195, 36], 'stop-mowing': [235, 342, 195, 36],
    sprinkler: [448, 300, 210, 36], 'change-Outside': [448, 342, 210, 36],
    plants: [448, 384, 210, 30],
  },
};

const roomChangeId = {
  LIVING_ROOM: 'change-LivingRoom', KITCHEN: 'change-Kitchen', BEDROOM: 'change-Bedroom',
  GARAGE: 'change-Garage', MOMS_ROOM: 'change-MomsRoom', BATHROOM: 'change-Bathroom', OUTSIDE: 'change-Outside',
};
const roomChangeFlag = {
  LIVING_ROOM: 'looseChangeFound_LivingRoom', KITCHEN: 'looseChangeFound_Kitchen',
  BEDROOM: 'looseChangeFound_Bedroom', GARAGE: 'looseChangeFound_Garage',
  MOMS_ROOM: 'looseChangeFound_MomsRoom', BATHROOM: 'looseChangeFound_Bathroom',
  OUTSIDE: 'looseChangeFound_Outside',
};

function sourceHides(room, id, s) {
  if (id === roomChangeId[room] && s[roomChangeFlag[room]]) return true;
  if (room === 'LIVING_ROOM') {
    if (id === 'tidy-living' && s.choreCleanLR) return true;
    if (['tv', 'vacuum', 'clean-mess'].includes(id) && s.squirrelPresent) return true;
    if (id === 'shoo' && s.squirrelPresent === false) return true;
    if (id === 'clean-mess' && s.mittensMessRoom !== undefined && s.mittensMessRoom !== room) return true;
    if (id.startsWith('ac-') && (s.acBroken || s.powerOut)) return true;
  }
  if (room === 'KITCHEN') {
    const bagPriority = s.choreTrashOut && !s.choreTrashBagIn;
    const deliveryPriority = s.deliveryArrived && s.hunger < 95;
    if (id === 'change-Kitchen' && bagPriority) return true;
    if (id === 'trash-bag' && s.choreTrashOut !== undefined && !bagPriority) return true;
    if (['freezer', 'ice-cream'].includes(id) && deliveryPriority) return true;
    if (id === 'pickup-food' && s.deliveryArrived !== undefined && !deliveryPriority) return true;
    if (id === 'freezer' && s.iceCreamFound) return true;
    if (id === 'ice-cream' && s.iceCreamFound === false) return true;
    if (id === 'eat-cold' && s.powerOut) return true;
  }
  if (room === 'BEDROOM') {
    if (id === 'stool' && s.stepStoolInBedroom === false && s.hasStepStool === false) return true;
    if (['change-clothes', 'nap'].includes(id) && s.needsTowelDry) return true;
    if (id === 'nap' && s.needsClothesChange) return true;
  }
  if (room === 'GARAGE') {
    if (id === 'wrench' && s.wrenchInGarage === false && s.hasWrench === false) return true;
    if (id === 'rats' && s.roachesPresent && !s.roachesCleared) return true;
    if (id === 'roaches-cleared' && ((s.roachesPresent && !s.roachesCleared) || (s.ratsPresent && !s.ratsClearedToday))) return true;
    if (id === 'circuit' && s.powerOut === false) return true;
  }
  if (room === 'MOMS_ROOM') {
    if (s.hidingInCloset && id !== 'leave-closet') return true;
    if (id === 'leave-closet' && s.hidingInCloset === false) return true;
    if (id === 'rest-mom' && (s.powerOut || s.hasMomFlashlight)) return true;
  }
  if (room === 'OUTSIDE') {
    if (id === 'trash' && s.choreTrashOut && s.choreTrashBagIn) return true;
    if (id === 'stop-mowing' && s.thunderstorm) return true;
  }
  return false;
}

/** Unknown/phone actions and controls absent in the source state return null. */
export function actionRect(roomId, id, state = {}) {
  if (typeof id !== 'string' || sourceHides(roomId, id, state)) return null;
  if (!Object.prototype.hasOwnProperty.call(rectangles, roomId)) return null;
  const room = rectangles[roomId];
  if (!Object.prototype.hasOwnProperty.call(room, id)) return null;
  let values = room[id];
  if (roomId === 'BASEMENT') {
    if (id === 'washer') {
      if (state.laundryState === 1) values = [260, 290, 280, 38];
      else if (state.laundryDetergent <= 0) values = [170, 290, 460, 38];
    }
    if (id === 'fold' && state.laundryState >= 5) values = [260, 290, 280, 38];
  }
  const [x, y, w, h] = values;
  return { x, y, w, h };
}

/** Keep source geometry and visibility without modifying the action descriptors. */
export function layoutActions(roomId, actions, state = {}) {
  return actions.flatMap(action => {
    const rect = actionRect(roomId, action.id, state);
    return rect ? [{ ...action, rect }] : [];
  });
}
