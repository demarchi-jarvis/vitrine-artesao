output "public_ip" {
  description = "IP público da instância EC2 (muda ao parar/iniciar no Academy)"
  value       = aws_instance.vitrine.public_ip
}

output "api_url" {
  description = "URL base da API Spring Boot"
  value       = "http://${aws_instance.vitrine.public_ip}:8081"
}

output "health_url" {
  description = "Endpoint de health check da API"
  value       = "http://${aws_instance.vitrine.public_ip}:8081/actuator/health"
}

output "ssh_command" {
  description = "Comando SSH para conectar na instância"
  value       = "ssh -i ~/vockey.pem ec2-user@${aws_instance.vitrine.public_ip}"
}

output "deploy_log" {
  description = "Comando para acompanhar o log de instalação em tempo real"
  value       = "ssh -i ~/vockey.pem ec2-user@${aws_instance.vitrine.public_ip} 'tail -f /var/log/userdata.log'"
}
