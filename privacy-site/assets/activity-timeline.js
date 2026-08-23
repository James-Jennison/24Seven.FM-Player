(() => {
  const localFormatter = new Intl.DateTimeFormat(undefined, {
    year: 'numeric', month: 'long', day: 'numeric',
    hour: 'numeric', minute: '2-digit', second: '2-digit', timeZoneName: 'short',
  });
  const utcFormatter = new Intl.DateTimeFormat(undefined, {
    hour: 'numeric', minute: '2-digit', second: '2-digit', timeZone: 'UTC', timeZoneName: 'short',
  });

  document.querySelectorAll('.activity-timeline time[datetime]').forEach((time) => {
    const moment = new Date(time.dateTime);
    if (Number.isNaN(moment.getTime())) return;
    const local = localFormatter.format(moment);
    const utc = utcFormatter.format(moment);
    time.textContent = local === utc ? local : `${local} · ${utc}`;
    time.setAttribute('aria-label', `${local}; ${utc}`);
  });

  document.querySelectorAll('.activity-timeline').forEach((timeline) => {
    const items = [...timeline.querySelectorAll('li[data-activity-kind]')];
    const empty = timeline.querySelector('[data-activity-empty]');
    const filters = [...timeline.querySelectorAll('[data-activity-filter]')];
    const apply = (kind) => {
      let visible = 0;
      items.forEach((item) => {
        const matches = kind === 'all' || item.dataset.activityKind === kind;
        item.hidden = !matches;
        if (matches) visible += 1;
      });
      if (empty) empty.hidden = visible !== 0;
      filters.forEach((filter) => filter.setAttribute('aria-pressed', String(filter.dataset.activityFilter === kind)));
    };
    filters.forEach((filter) => filter.addEventListener('click', () => apply(filter.dataset.activityFilter || 'all')));
  });
})();
