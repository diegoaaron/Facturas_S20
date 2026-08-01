export function formatCurrency(amount: number, moneda: string): string {
  const symbol = moneda.toUpperCase() === "USD" ? "$" : "S/";
  return `${symbol} ${amount.toFixed(2)}`;
}

const AVATAR_COLORS = [
  "#1B4D45",
  "#2E6E5E",
  "#8BC34A",
  "#E85D2C",
  "#2E7D32",
  "#6B6B6B",
  "#0E6B77",
  "#A1458E",
];

export function avatarColor(name: string): string {
  let hash = 0;
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash);
  }
  const index = Math.abs(hash) % AVATAR_COLORS.length;
  return AVATAR_COLORS[index];
}

export function initial(name: string): string {
  return name.trim().charAt(0).toUpperCase() || "?";
}

export function relativeDate(isoDate: string): string {
  const date = new Date(isoDate);
  const now = new Date();
  const startOfDay = (d: Date) => new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();
  const diffDays = Math.round((startOfDay(now) - startOfDay(date)) / (1000 * 60 * 60 * 24));

  if (diffDays === 0) {
    return `Hoy, ${formatDocDate(date)}`;
  }
  if (diffDays === 1) {
    return `Ayer, ${formatDocDate(date)}`;
  }
  return formatDocDate(date);
}

function formatDocDate(date: Date): string {
  const day = String(date.getDate()).padStart(2, "0");
  const months = [
    "ENE", "FEB", "MAR", "ABR", "MAY", "JUN",
    "JUL", "AGO", "SEP", "OCT", "NOV", "DIC",
  ];
  return `${day}${months[date.getMonth()]}`;
}
