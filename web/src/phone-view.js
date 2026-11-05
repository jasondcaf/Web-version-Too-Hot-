// PhonePanel.java's app hierarchy and painted phone content, expressed as HTML.
// Game rules stay in actions.js; these renderers only select existing actions.
const escape = value => String(value ?? '').replace(/[&<>"']/g, c => ({
  '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
}[c]));

const titles = {
  HOME: 'Phone', MESSAGES: 'Messages', MSG_MOM: 'Mom', MSG_FRIEND: 'Friend',
  DELIVERY: 'Delivery', MUSIC: 'Music', GAMES: 'Flappy Bird',
};
const apps = [
  ['MESSAGES', 'Messages', 'messages'], ['DELIVERY', 'Delivery', 'delivery'],
  ['MUSIC', 'Music', 'music'], ['GAMES', 'Games', 'games'],
];

function actionButton(actions, id, label, className = '') {
  const action = actions.find(a => a.id === id);
  if (!action) return '';
  return `<button type="button" class="phone-action ${escape(className)}" data-action="${escape(action.id)}"${action.enabled === false ? ' disabled' : ''}>${escape(label ?? action.label)}</button>`;
}

function screenButton(screen, label, className = '', disabled = false) {
  return `<button type="button" class="${escape(className)}" data-phone="${escape(screen)}"${disabled ? ' disabled' : ''}>${label}</button>`;
}

export function parentPhoneScreen(screen) {
  if (screen === 'HOME') return 'PUT_AWAY';
  return screen === 'MSG_MOM' || screen === 'MSG_FRIEND' ? 'MESSAGES' : 'HOME';
}

export function phoneScreenTitle(screen) { return titles[screen] || titles.HOME; }

/** Safe HTML for live delivery updates; elapsed time does not reset selection. */
export function renderPhoneDeliveryStatus(s) {
  const lines = [];
  if (s.deliveryOrdered && !s.deliveryArrived) {
    lines.push(`🚚 En route… (${Math.max(0, Math.floor(s.deliveryTicks / 2))}s)`);
  } else if (s.deliveryArrived && s.deliveryItemType === 'Food') {
    lines.push('✅ Food in Kitchen!');
  }
  if (s.detergentOrdered && !s.detergentArrived) {
    lines.push(`🚚 Detergent en route… (${Math.max(0, Math.floor(s.detergentTicks / 2))}s)`);
  } else if (s.detergentArrived) lines.push('✅ Detergent in Basement!');
  if (s.coldMedOrdered && !s.coldMedArrived) {
    lines.push(`🚚 Medicine en route… (${Math.max(0, Math.floor(s.coldMedTicks / 2))}s)`);
  } else if (s.coldMedArrived) lines.push('✅ Medicine Arrived! (take it here)');
  if (s.hasSpeaker) lines.push('🔊 Speaker active — music boost!');
  return lines.map(line => `<p class="phone-delivery-status">${escape(line)}</p>`).join('');
}

export function renderPhoneContent(s, actions, screen = 'HOME', selectedDelivery = 'order-Food') {
  if (!Object.hasOwn(titles, screen)) screen = 'HOME';
  const power = s.phoneBattery > 5;
  let html = '';
  if (screen === 'HOME') {
    html = `<div class="phone-app-grid">${apps.map(([id, label, type]) => {
      const notification = (id === 'MESSAGES' && s.friendTextPending) || (id === 'MUSIC' && s.musicPlaying);
      return screenButton(id, `<span class="phone-app-circle" aria-hidden="true"></span><span>${label}</span>${notification ? '<span class="phone-app-dot" aria-label="Notification"></span>' : ''}`, `phone-app phone-app-${type}`, !power);
    }).join('')}</div>`;
    if (s.phoneBattery >= 100) html += '<p class="phone-fully-charged">Phone: Fully Charged ✓</p>';
    if (s.friendTextPending) html += screenButton('MSG_FRIEND', '📩 New text from Friend!', 'phone-notification', !power);
    if (!power) html += '<p class="phone-dead">No battery — charge the phone!</p>';
  } else if (screen === 'MESSAGES') {
    html = `<div class="phone-contacts">${screenButton('MSG_MOM', '<span class="phone-contact-avatar phone-mom" aria-hidden="true">M</span><span>Mom</span>', 'phone-contact')}${screenButton('MSG_FRIEND', `<span class="phone-contact-avatar phone-friend" aria-hidden="true">F</span><span>Friend${s.friendTextPending ? ' <small>[new!]</small>' : ''}</span>`, 'phone-contact')}</div>`;
  } else if (screen === 'MSG_MOM') {
    html = `<div class="phone-conversation"><div class="phone-avatar phone-mom" aria-hidden="true">M</div><div class="phone-chat-divider"></div><p class="phone-chat-bubble">Behave! I'll be home soon ❤</p><div class="phone-app-actions">${actionButton(actions, 'call-mom', '📞 Call Mom', 'phone-mom-action')}${actionButton(actions, 'text-mom', '💬 Text Mom')}</div></div>`;
  } else if (screen === 'MSG_FRIEND') {
    html = `<div class="phone-conversation"><div class="phone-avatar phone-friend" aria-hidden="true">F</div><div class="phone-chat-divider"></div>${s.friendTextPending ? '<p class="phone-chat-bubble phone-chat-unread">Yo what\'s up, u home alone?</p>' : '<div class="phone-chat-placeholder" aria-hidden="true"></div>'}<div class="phone-app-actions">${actionButton(actions, 'call-friend', '📞 Call Friend', 'phone-friend-action')}${s.friendTextPending ? actionButton(actions, 'reply-friend', '💬 Reply to Text', 'phone-reply-action') + actionButton(actions, 'ignore-friend', '🚫 Ignore', 'phone-ignore-action') : actionButton(actions, 'text-friend', '💬 Text Friend')}</div></div>`;
  } else if (screen === 'DELIVERY') {
    const orders = actions.filter(a => a.id.startsWith('order-'));
    const selectedId = selectedDelivery.startsWith('order-') ? selectedDelivery : `order-${selectedDelivery}`;
    const selected = orders.find(a => a.id === selectedId) || orders[0];
    const cost = selected?.label.match(/\((\d+) coins\)/)?.[1] || '';
    html = `<div class="phone-delivery"><label class="phone-select-label" for="phone-delivery">Choose an item</label><select id="phone-delivery" aria-label="Delivery item">${orders.map(a => `<option value="${escape(a.id)}"${a === selected ? ' selected' : ''}>${escape(a.label.replace(/^Order /, '').replace(/ \((\d+) coins\)/, ' — $1 coins'))}</option>`).join('')}</select>${selected ? `<button type="button" id="phone-order" class="phone-action phone-order" data-action="${escape(selected.id)}"${selected.enabled === false ? ' disabled' : ''}>Order${cost ? ` (${escape(cost)} coins)` : ''}</button>` : ''}<div id="delivery-eta" aria-live="polite">${renderPhoneDeliveryStatus(s)}</div>${actionButton(actions, 'take-cold-medicine', 'Take Cold Medicine', 'phone-reply-action')}</div>`;
  } else if (screen === 'MUSIC') {
    const genres = [['Pop', '🎸', 'pop'], ['Hip-Hop', '🎤', 'hiphop'], ['Classical', '🎻', 'classical'], ['Lo-fi', '🎧', 'lofi']];
    html = `<div class="phone-app-actions phone-music-actions">${genres.map(([name, icon, type]) => actionButton(actions, `music-${name}`, `${icon} ${name}${s.musicPlaying && s.musicGenre === name ? ' ♫ playing' : ''}`, `phone-genre-${type}${s.musicPlaying && s.musicGenre === name ? ' phone-genre-playing' : ''}`)).join('')}</div>${s.musicPlaying ? `<p class="phone-now-playing">♫ Now Playing: ${escape(s.musicGenre)}</p>${s.hasSpeaker ? '<p class="phone-speaker-boost">🔊 Speaker boost active!</p>' : ''}` : ''}`;
  } else if (screen === 'GAMES') {
    html = power ? '<div id="flappy"></div>' : '<p class="phone-dead">No battery — charge the phone!</p>';
  }
  return `<div class="phone-app-view phone-view-${screen.toLowerCase()}"><h2 class="phone-app-title">${titles[screen]}</h2>${html}</div>`;
}
