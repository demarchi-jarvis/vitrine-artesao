output "public_ip" {
  description = "IP público da instância EC2 (ATENÇÃO: muda ao parar/iniciar a instância no Academy)"
  value       = aws_instance.vitrine.public_ip
}

output "api_url" {
  description = "URL base da API Spring Boot"
  value       = "http://${aws_instance.vitrine.public_ip}:8081"
}

output "health_url" {
  description = "Endpoint de health check — verifique aqui se o deploy concluiu"
  value       = "http://${aws_instance.vitrine.public_ip}:8081/actuator/health"
}

output "ssh_command" {
  description = "Comando SSH para conectar (substitua pelo caminho correto do .pem)"
  value       = "ssh -i ~/vockey.pem ec2-user@${aws_instance.vitrine.public_ip}"
}

output "deploy_log" {
  description = "Acompanhe o progresso do bootstrap em tempo real"
  value       = "ssh -i ~/vockey.pem ec2-user@${aws_instance.vitrine.public_ip} 'tail -f /var/log/userdata.log'"
}

output "nota_academy" {
  description = "Aviso sobre credenciais temporárias"
  value       = "IMPORTANTE: No AWS Academy, o IP público muda a cada reinício da instância. Renove as credenciais do Lab (AWS_SESSION_TOKEN) antes de rodar terraform apply novamente."
}
