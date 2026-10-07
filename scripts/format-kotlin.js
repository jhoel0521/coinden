const { execSync } = require('child_process');
const os = require('os');

const isWindows = os.platform() === 'win32';
const gradlewCmd = isWindows ? '.\\gradlew.bat' : './gradlew';

try {
  console.log('🔍 Ejecutando ktlintFormat...');
  execSync(`${gradlewCmd} ktlintFormat`, { stdio: 'inherit' });
  console.log('✅ Formato de Kotlin aplicado correctamente.');
} catch (error) {
  console.error('❌ Error formateando código Kotlin:', error.message);
  process.exit(1);
}
